package application;

import application.ports.*;
import domain.model.*;
import domain.model.bike.*;
import domain.model.repository.*;
import domain.model.simulation.*;
import io.vertx.core.Vertx;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestAutonomousRideServiceImpl implements RestAutonomousRideService {

  private final Logger logger = LoggerFactory.getLogger(RestAutonomousRideServiceImpl.class);
  private final RideRepository rideRepository;
  private final EventPublisher eventPublisher;
  private final MapCommunicationPort mapCommunicationAdapter;
  private final UserCommunicationPort userCommunicationAdapter;
  private final ABikeRepository abikeRepository;
  private final UserRepository userRepository;
  private final Vertx vertx;
  private final StationRepository stationRepository;

  public RestAutonomousRideServiceImpl(
      EventPublisher publisher,
      Vertx vertx,
      MapCommunicationPort mapCommunicationAdapter,
      UserCommunicationPort userCommunicationAdapter,
      ABikeRepository abikeRepository,
      UserRepository userRepository,
      StationRepository stationRepository) {
    this.vertx = vertx;
    this.eventPublisher = publisher;
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.userCommunicationAdapter = userCommunicationAdapter;
    this.abikeRepository = abikeRepository;
    this.userRepository = userRepository;
    this.stationRepository = stationRepository;
  }

  @Override
  public CompletableFuture<Void> dispatchBikeToUser(
      String userId, String bikeId, P2d userLocation) {
    CompletableFuture<ABike> bikeFuture = checkABike(bikeId);
    CompletableFuture<User> userFuture = checkUser(userId);

    return CompletableFuture.allOf(bikeFuture, userFuture)
        .thenCompose(
            v -> {
              ABike bike = bikeFuture.join();
              User user = userFuture.join();

              if (bike == null || user == null) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("ABike or User not found"));
              } else if (bike.getState() != ABikeState.AVAILABLE) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("ABike is not available"));
              } else if (user.getCredit() == 0) {
                return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
              } else if (bike.getBatteryLevel() == 0) {
                return CompletableFuture.failedFuture(new RuntimeException("ABike has no battery"));
              }

              userCommunicationAdapter.addDispatch(user.getId(), bike.getId(), userLocation);

              // Create the base ride
              String rideId = "ride-" + userId + "-" + bikeId + "-combined";
              Ride ride = new Ride(rideId, user, bike);

              // Create both simulations
              AutonomousRideSimulation autonomousSim =
                  new AutonomousRideSimulation(
                      ride, vertx, eventPublisher, userLocation, AutonomousRideType.TO_USER);

              NormalRideSimulation normalSim =
                  new NormalRideSimulation(ride, vertx, eventPublisher);

              // Update the dispatchBikeToUser method to use the new builder pattern
              SequentialRideSimulation sequentialSim =
                  SequentialRideSimulation.builder(rideId, vertx, eventPublisher)
                      .addStage(
                          autonomousSim,
                          (completedSim, nextSim) -> {
                            // This runs when the autonomous simulation completes
                            userCommunicationAdapter.removeDispatch(
                                user.getId(), bike.getId(), true);
                          })
                      .addStage(normalSim, (completedSim, nextSim) -> {})
                      .build();

              rideRepository.addRideWithSimulation(ride, sequentialSim);

              mapCommunicationAdapter.notifyStartRide(bikeId, bike.getType(), userId);

              // Start the sequential simulation
              sequentialSim
                  .startSimulation(Optional.of(ABikeState.MOVING_TO_USER))
                  .whenComplete(
                      (res, err) -> {
                        if (err == null) {
                          logger.info("Sequential simulation completed successfully");
                          if (!sequentialSim.isStoppedManually()) {
                            mapCommunicationAdapter.notifyEndRide(bikeId, bike.getType(), userId);
                            ;
                            rideRepository.removeRide(ride);
                            if (bike.getState().equals(ABikeState.MAINTENANCE)) {
                              userCommunicationAdapter.removeDispatch(
                                  user.getId(), bike.getId(), false);
                            }
                          }

                        } else {
                          logger.error(
                              "Error during sequential simulation: " + err.getMessage(), err);
                        }
                      });

              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<Void> stopAutonomousRide(String userId) {
    return CompletableFuture.supplyAsync(() -> rideRepository.getRideSimulationByUserId(userId))
        .thenCompose(
            rideSimulation -> {
              if (rideSimulation != null) {
                logger.info("Stopping autonomous ride for user: {}", userId);

                Ride ride = rideSimulation.getRide();

                if (rideSimulation instanceof SequentialRideSimulation sequentialSim) {
                  RideSimulation currentSim = sequentialSim.getCurrentSimulation();
                  if (currentSim instanceof AutonomousRideSimulation) {
                    userCommunicationAdapter.removeDispatch(userId, ride.getBike().getId(), false);
                  } else if (currentSim instanceof NormalRideSimulation) {
                    userCommunicationAdapter.removeDispatch(userId, ride.getBike().getId(), true);
                  }
                }

                rideSimulation.stopSimulationManually();

                mapCommunicationAdapter.notifyEndRide(
                    ride.getBike().getId(), BikeType.AUTONOMOUS, userId);

                rideRepository.removeRide(ride);

                P2d bikePosition = ride.getBike().getLocation();
                P2d closestStationLocation =
                    stationRepository
                        .findClosestStation(bikePosition)
                        .thenApply(
                            stationOpt ->
                                stationOpt
                                    .map(
                                        station ->
                                            new P2d(
                                                station.getJsonObject("location").getDouble("x"),
                                                station.getJsonObject("location").getDouble("y")))
                                    .orElse(new P2d(0, 0))) // Default to (0,0) if no station found
                        .join();
                // Poll every 500ms to check if the ABike came back available from ABike-Service
                vertx.setPeriodic(
                    500,
                    id -> {
                      abikeRepository
                          .findById(ride.getBike().getId())
                          .thenAccept(
                              abikeJsonOpt -> {
                                if (abikeJsonOpt.isPresent()) {
                                  //                                  JsonObject abikeJson =
                                  // abikeJsonOpt.get();
                                  var aBike = abikeJsonOpt.get();
                                  ABikeState state = (ABikeState) aBike.getState();
                                  if (state == ABikeState.AVAILABLE) {
                                    vertx.cancelTimer(id);
                                    dispatchBikeToStation(
                                            userId, ride.getBike().getId(), closestStationLocation)
                                        .whenComplete(
                                            (res, err) -> {
                                              if (err != null) {
                                                logger.error(
                                                    "Error dispatching bike to station: "
                                                        + err.getMessage(),
                                                    err);
                                              } else {
                                                logger.info(
                                                    "Bike dispatched to station successfully");
                                              }
                                            });
                                  }
                                }
                              });
                    });

              } else {
                logger.warn("No active ride found for user: {}", userId);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  private CompletableFuture<Void> dispatchBikeToStation(
      String userId, String bikeId, P2d stationLocation) {
    CompletableFuture<ABike> bikeFuture = checkABike(bikeId);
    CompletableFuture<User> userFuture = checkUser(userId);
    CompletableFuture<String> stationIdFuture =
        stationRepository
            .findClosestStation(stationLocation)
            .thenApply(
                stationOpt ->
                    stationOpt.map(station -> station.getString("id")).orElse("unknown-station"));

    return CompletableFuture.allOf(bikeFuture, userFuture)
        .thenCompose(
            v -> {
              ABike bike = bikeFuture.join();
              User user = userFuture.join();
              String stationId = stationIdFuture.join();

              if (bike == null || user == null || stationId == null) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("ABike or User not found"));
              } else if (bike.getState() != ABikeState.AVAILABLE) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("ABike is not available"));
              } else if (stationId.equals("unknown-station")) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("No station found near the bike"));
              } else if (user.getCredit() == 0) {
                return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
              } else if (bike.getBatteryLevel() == 0) {
                return CompletableFuture.failedFuture(new RuntimeException("ABike has no battery"));
              }

              String rideId = "ride-" + userId + "-" + bikeId + "-station";
              Ride ride = new Ride(rideId, user, bike);

              AutonomousRideSimulation autonomousSim =
                  new AutonomousRideSimulation(
                      ride, vertx, eventPublisher, stationLocation, AutonomousRideType.TO_STATION);

              rideRepository.addRideWithSimulation(ride, autonomousSim);

              mapCommunicationAdapter.notifyStartPublicRide(bikeId, bike.getType());

              autonomousSim
                  .startSimulation(Optional.of(ABikeState.MOVING_TO_STATION))
                  .whenComplete(
                      (res, err) -> {
                        if (err == null) {
                          logger.info("Autonomous simulation completed successfully");
                          mapCommunicationAdapter.notifyEndPublicRide(bikeId, bike.getType());
                          rideRepository.removeRide(ride);

                          if (autonomousSim.isDestinationReached()) {
                            eventPublisher.publishABikeStationUpdate(bikeId, stationId);
                          } else {
                            logger.info("Autonomous simulation did not make it to destination");
                          }
                        } else {
                          logger.error(
                              "Error during autonomous simulation: " + err.getMessage(), err);
                        }
                      });

              return CompletableFuture.completedFuture(null);
            });
  }

  private CompletableFuture<User> checkUser(String userId) {
    System.out.println("Checking user: " + userId);

    Optional<User> user = userRepository.findById(userId);
    if (user.isPresent()) {
      return CompletableFuture.completedFuture(user.get());
    } else {
      System.err.println("User not found");
      return CompletableFuture.completedFuture(null);
    }
  }

  private CompletableFuture<ABike> checkABike(String bikeId) {
    System.out.println("Checking abike: " + bikeId);

    return abikeRepository
        .findById(bikeId)
        .thenApply(
            abikeOptional -> {
              if (abikeOptional.isEmpty()) {
                System.err.println("ABike not found");
                return null;
              } else {
                return abikeOptional.get();
              }
            });
  }
}
