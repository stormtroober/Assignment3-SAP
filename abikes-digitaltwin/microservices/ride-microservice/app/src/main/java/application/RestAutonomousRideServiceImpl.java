package application;

import application.ports.*;
import domain.model.*;
import domain.model.bike.*;
import domain.model.repository.*;
import domain.model.simulation.AutonomousRideSimulation;
import domain.model.simulation.NormalRideSimulation;
import domain.model.simulation.RideSimulation;
import domain.model.simulation.SequentialRideSimulation;
import infrastructure.repository.DispatchRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RestAutonomousRideServiceImpl implements RestAutonomousRideService {

  private final Logger logger = LoggerFactory.getLogger(RestAutonomousRideServiceImpl.class);
  private final RideRepository rideRepository;
  private final BikeCommunicationPort bikeCommunicationAdapter;
  private EventPublisher eventPublisher;
  private final MapCommunicationPort mapCommunicationAdapter;
  private final UserCommunicationPort userCommunicationAdapter;
  private final ABikeRepository abikeRepository;
  private final UserRepository userRepository;
    private final DispatchRepository dispatchRepository;
  private final Vertx vertx;

  public RestAutonomousRideServiceImpl(
      EventPublisher publisher,
      Vertx vertx,
      BikeCommunicationPort bikeCommunicationAdapter,
      MapCommunicationPort mapCommunicationAdapter,
      UserCommunicationPort userCommunicationAdapter,
      ABikeRepository abikeRepository,
      UserRepository userRepository,
      DispatchRepository dispatchRepository) {
    this.vertx = vertx;
    this.eventPublisher = publisher;
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.userCommunicationAdapter = userCommunicationAdapter;
    this.abikeRepository = abikeRepository;
    this.userRepository = userRepository;
    this.dispatchRepository = dispatchRepository;
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
                        new AutonomousRideSimulation(ride, vertx, eventPublisher, userLocation);

                NormalRideSimulation normalSim = new NormalRideSimulation(ride, vertx, eventPublisher);

                // Update the dispatchBikeToUser method to use the new builder pattern
                SequentialRideSimulation sequentialSim = SequentialRideSimulation.builder(
                                rideId,
                                vertx,
                                eventPublisher)
                        .addStage(
                                autonomousSim,
                                (completedSim, nextSim) -> {
                                    // This runs when the autonomous simulation completes
                                    userCommunicationAdapter.removeDispatch(user.getId(), bike.getId(), true);
                                })
                        .addStage(
                                normalSim,
                                (completedSim, nextSim) -> {

                                })
                        .build();

                rideRepository.addRideWithSimulation(ride, sequentialSim);

                mapCommunicationAdapter.notifyStartRide(bikeId, bike.getType(), userId);

                // Start the sequential simulation
                sequentialSim.startSimulation(Optional.of(ABikeState.MOVING_TO_USER))
                        .whenComplete(
                                (res, err) -> {
                                    if (err == null) {
                                        logger.info("Sequential simulation completed successfully");
                                        if(!sequentialSim.isStoppedManually()){
                                            mapCommunicationAdapter.notifyEndRide(
                                                    bikeId, bike.getType(), userId);;
                                            rideRepository.removeRide(ride);
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

                                bikeCommunicationAdapter.sendUpdateABike(
                                        new JsonObject()
                                                .put("id", ride.getBike().getId())
                                                .put("state", ride.getBike().getState().toString()));
                                mapCommunicationAdapter.notifyEndRide(
                                        ride.getBike().getId(), BikeType.AUTONOMOUS, userId);

                                rideRepository.removeRide(ride);

                                // Poll every 500ms to check if the ABike came back available from ABike-Service
                                vertx.setPeriodic(500, id -> {
                                    abikeRepository.findById(ride.getBike().getId()).thenAccept(abikeJsonOpt -> {
                                        if (abikeJsonOpt.isPresent()) {
                                            JsonObject abikeJson = abikeJsonOpt.get();
                                            ABikeState state = ABikeState.valueOf(abikeJson.getString("state"));
                                            if (state == ABikeState.AVAILABLE) {
                                                vertx.cancelTimer(id);
                                                // Replace with actual station location
                                                P2d stationLocation = new P2d(0, 0); // Example location
                                                dispatchBikeToStation(userId, ride.getBike().getId(), stationLocation).whenComplete(
                                                        (res, err) -> {
                                                    if (err != null) {
                                                        logger.error("Error dispatching bike to station: " + err.getMessage(), err);
                                                    } else {
                                                        logger.info("Bike dispatched to station successfully");
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

    private CompletableFuture<Void> dispatchBikeToStation(String userId, String bikeId, P2d stationLocation) {
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

                            String rideId = "ride-" + userId + "-" + bikeId + "-station";
                            Ride ride = new Ride(rideId, user, bike);

                            AutonomousRideSimulation autonomousSim =
                                    new AutonomousRideSimulation(ride, vertx, eventPublisher, stationLocation);

                            rideRepository.addRideWithSimulation(ride, autonomousSim);

                            autonomousSim.startSimulation(Optional.of(ABikeState.MOVING_TO_STATION)).whenComplete(
                                    (res, err) -> {
                                        if (err == null) {
                                            logger.info("Autonomous simulation completed successfully");
//                                            mapCommunicationAdapter.notifyEndRide(
//                                                    bikeId, bike.getType(), userId);
                                            rideRepository.removeRide(ride);
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
            abikeJsonOptional -> {
              if (abikeJsonOptional.isEmpty()) {
                System.err.println("ABike not found");
                return null;
              }

              JsonObject abikeJson = abikeJsonOptional.get();
              JsonObject location = abikeJson.getJsonObject("location");
              P2d loc = new P2d(location.getDouble("x"), location.getDouble("y"));
              ABikeState state = ABikeState.valueOf(abikeJson.getString("state"));
              int batteryLevel = abikeJson.getInteger("batteryLevel");
              BikeType type = BikeType.valueOf(abikeJson.getString("type"));
              return ABikeFactory.getInstance()
                  .create(abikeJson.getString("id"), loc, state, batteryLevel, type);
            });
  }
}
