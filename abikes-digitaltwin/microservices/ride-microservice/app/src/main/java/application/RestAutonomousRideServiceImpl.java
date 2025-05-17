package application;

import application.ports.*;
import domain.model.*;
import domain.model.bike.ABike;
import domain.model.bike.ABikeFactory;
import domain.model.bike.ABikeState;
import domain.model.bike.BikeType;
import domain.model.repository.*;
import domain.model.simulation.RideSimulation;
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
  private final MapCommunicationPort mapCommunicationAdapter;
  private final UserCommunicationPort userCommunicationAdapter;
  private final ABikeRepository abikeRepository;
  private final UserRepository userRepository;

  public RestAutonomousRideServiceImpl(
      EventPublisher publisher,
      Vertx vertx,
      BikeCommunicationPort bikeCommunicationAdapter,
      MapCommunicationPort mapCommunicationAdapter,
      UserCommunicationPort userCommunicationAdapter,
      ABikeRepository abikeRepository,
      UserRepository userRepository) {
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.userCommunicationAdapter = userCommunicationAdapter;
    this.abikeRepository = abikeRepository;
    this.userRepository = userRepository;
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

              JsonObject userJson = new JsonObject();
              userJson.put("userId", user.getId());
              userJson.put("bikeId", bikeId);
              userJson.put("positionX", userLocation.x());
              userJson.put("positionY", userLocation.y());
              logger.info("Dispatch for user: {}", userJson.encodePrettily());
              // To have the dot of user in the map
              userCommunicationAdapter.sendDispatchToRide(userJson);
              // create ride and simulation
              Ride ride =
                  new Ride(
                      "ride-" + userId + "-" + bikeId + "-" + SimulationType.AUTONOMOUS_SIM,
                      user,
                      bike);
              rideRepository.addRide(
                  ride, SimulationType.AUTONOMOUS_SIM, Optional.of(userLocation));

              RideSimulation sim = rideRepository.getRideSimulation(ride.getId());
              // start simulation and notify on completion
              sim.startSimulation()
                  .whenComplete(
                      (res, err) -> {
                        if (err == null) {
                          mapCommunicationAdapter.notifyStopRideToUser(
                              bikeId, bike.getType(), userId);
                          rideRepository.removeRide(ride);
                        } else {
                          System.err.println(
                              "Error during autonomous simulation: " + err.getMessage());
                        }
                      });

              mapCommunicationAdapter.notifyStartRideToUser(bikeId, bike.getType(), userId);

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
