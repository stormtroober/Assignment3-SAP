package application;

import application.ports.*;
import domain.model.*;
import domain.model.bike.ABike;
import domain.model.bike.ABikeFactory;
import domain.model.bike.ABikeState;
import domain.model.bike.BikeType;
import domain.model.repository.RideRepository;
import domain.model.repository.RideRepositoryImpl;
import domain.model.repository.SimulationType;
import domain.model.simulation.RideSimulation;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestAutonomousRideServiceImpl implements RestAutonomousRideService {

  private final RideRepository rideRepository;
  private final BikeCommunicationPort abikeCommunicationAdapter;
  private final MapCommunicationPort mapCommunicationAdapter;
  private final UserCommunicationPort userCommunicationAdapter;

  public RestAutonomousRideServiceImpl(
      EventPublisher publisher,
      Vertx vertx,
      BikeCommunicationPort abikeCommunicationAdapter,
      MapCommunicationPort mapCommunicationAdapter,
      UserCommunicationPort userCommunicationAdapter) {
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.abikeCommunicationAdapter = abikeCommunicationAdapter;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.userCommunicationAdapter = userCommunicationAdapter;
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
    return userCommunicationAdapter
        .getUser(userId)
        .thenApply(
            userJson -> {
              if (userJson == null) {
                System.err.println("User not found");
                return null;
              }

              return new User(userJson.getString("username"), userJson.getInteger("credit"));
            });
  }

  private CompletableFuture<ABike> checkABike(String bikeId) {
    System.out.println("Checking abike: " + bikeId);
    return abikeCommunicationAdapter
        .getBike(bikeId)
        .thenApply(
            abikeJson -> {
              if (abikeJson == null) {
                System.err.println("ABike not found");
                return null;
              }
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
