package application;

import application.ports.*;
import domain.model.*;
import domain.model.bike.BikeType;
import domain.model.bike.EBike;
import domain.model.bike.EBikeState;
import domain.model.repository.*;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestSimpleRideServiceImpl implements RestSimpleRideService {

  private final EBikeRepository ebikeRepository;
  private final UserRepository userRepository;
  private final RideRepository rideRepository;
  private final BikeCommunicationPort bikeCommunicationAdapter;
  private final MapCommunicationPort mapCommunicationAdapter;

  public RestSimpleRideServiceImpl(
      EventPublisher publisher,
      Vertx vertx,
      UserRepository userRepository,
      EBikeRepository ebikeRepository,
      BikeCommunicationPort bikeCommunicationAdapter,
      MapCommunicationPort mapCommunicationAdapter) {
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.userRepository = userRepository;
    this.ebikeRepository = ebikeRepository;
  }

  private CompletableFuture<EBike> checkEbike(String bikeId) {
    System.out.println("Checking ebike: " + bikeId);
    return ebikeRepository
        .findById(bikeId)
        .thenApply(
            ebikeJsonOptional -> {
              if (ebikeJsonOptional.isEmpty()) {
                System.err.println("EBike not found");
                return null;
              }

              JsonObject ebikeJson = ebikeJsonOptional.get();
              JsonObject location = ebikeJson.getJsonObject("location");
              return new EBike(
                  ebikeJson.getString("id"),
                  location.getDouble("x"),
                  location.getDouble("y"),
                  EBikeState.valueOf(ebikeJson.getString("state")),
                  ebikeJson.getInteger("batteryLevel"));
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

  @Override
  public CompletableFuture<Void> startRide(String userId, String bikeId) {
    CompletableFuture<EBike> ebikeFuture = checkEbike(bikeId);
    CompletableFuture<User> userFuture = checkUser(userId);

    return CompletableFuture.allOf(ebikeFuture, userFuture)
        .thenCompose(
            v -> {
              try {
                EBike ebike = ebikeFuture.join();
                User user = userFuture.join();

                if (ebike == null || user == null) {
                  return CompletableFuture.failedFuture(
                      new RuntimeException("EBike or User not found"));
                } else if (ebike.getState() != EBikeState.AVAILABLE) {
                  return CompletableFuture.failedFuture(
                      new RuntimeException("EBike is not available"));
                } else if (user.getCredit() == 0) {
                  return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
                } else if (ebike.getBatteryLevel() == 0) {
                  return CompletableFuture.failedFuture(
                      new RuntimeException("EBike has no battery"));
                }

                System.out.println(
                    "Starting ride for user: "
                        + userId
                        + " and bike: "
                        + bikeId
                        + "-"
                        + SimulationType.NORMAL_SIM);
                Ride ride =
                    new Ride(
                        "ride-" + userId + "-" + bikeId + "-" + SimulationType.NORMAL_SIM,
                        user,
                        ebike);
                rideRepository.addRide(ride, SimulationType.NORMAL_SIM, Optional.empty());
                rideRepository
                    .getRideSimulation(ride.getId())
                    .startSimulation(Optional.empty())
                    .whenComplete(
                        (result, throwable) -> {
                          if (throwable == null) {
                            mapCommunicationAdapter.notifyEndRide(bikeId, BikeType.NORMAL, userId);
                            rideRepository.removeRide(ride);
                          } else {
                            System.err.println(
                                "Error during ride simulation: " + throwable.getMessage());
                          }
                        });
                mapCommunicationAdapter.notifyStartRide(bikeId, BikeType.NORMAL, userId);
                return CompletableFuture.completedFuture(null);
              } catch (Exception e) {
                return CompletableFuture.failedFuture(e);
              }
            });
  }

  @Override
  public CompletableFuture<Void> stopRide(String userId) {
    return CompletableFuture.supplyAsync(() -> rideRepository.getRideSimulationByUserId(userId))
        .thenCompose(
            rideSimulation -> {
              if (rideSimulation != null) {
                rideSimulation.stopSimulationManually();
                bikeCommunicationAdapter.sendUpdateEBike(
                    new JsonObject()
                        .put("id", rideSimulation.getRide().getBike().getId())
                        .put("state", rideSimulation.getRide().getBike().getState().toString()));
                mapCommunicationAdapter.notifyEndRide(
                    rideSimulation.getRide().getBike().getId(), BikeType.NORMAL, userId);
                rideRepository.removeRide(rideSimulation.getRide());
              }
              return CompletableFuture.completedFuture(null);
            });
  }
}
