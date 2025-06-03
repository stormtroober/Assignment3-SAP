package application;

import application.ports.*;
import domain.model.*;
import domain.model.repository.EBikeRepository;
import domain.model.repository.RideRepository;
import domain.model.repository.RideRepositoryImpl;
import domain.model.repository.UserRepository;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class RestRideServiceAPIImpl implements RestRideServiceAPI {
  private final RideRepository rideRepository;
  private final Vertx vertx;
  private final MapCommunicationPort mapCommunicationAdapter;
  private final EBikeRepository bikeRepository;
  private final UserRepository userRepository;
  private final EbikeCommunicationPort ebikeCommunicationAdapter;

  public RestRideServiceAPIImpl(
      EventPublisher publisher,
      Vertx vertx,
      EBikeRepository ebikeRepository,
      UserRepository userRepository,
      MapCommunicationPort mapCommunicationAdapter,
      EbikeCommunicationPort ebikeCommunicationAdapter) {
    this.rideRepository = new RideRepositoryImpl(vertx, publisher);
    this.vertx = vertx;
    this.bikeRepository = ebikeRepository;
    this.userRepository = userRepository;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.ebikeCommunicationAdapter = ebikeCommunicationAdapter;
  }

  private CompletableFuture<EBike> checkEbike(String bikeId) {
    System.out.println("Checking ebike: " + bikeId);
    return bikeRepository
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
                System.out.println("Starting ride for user: " + userId + " and bike: " + bikeId);
                Ride ride = new Ride("ride-" + userId + "-" + bikeId, user, ebike);
                rideRepository.addRide(ride);
                rideRepository
                    .getRideSimulation(ride.getId())
                    .startSimulation()
                    .whenComplete(
                        (result, throwable) -> {
                          if (throwable == null) {
                            mapCommunicationAdapter.notifyEndRide(bikeId, userId);
                            rideRepository.removeRide(ride);
                          } else {
                            System.err.println(
                                "Error during ride simulation: " + throwable.getMessage());
                          }
                        });
                mapCommunicationAdapter.notifyStartRide(bikeId, userId);
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
                ebikeCommunicationAdapter.sendUpdate(
                    new JsonObject()
                        .put("id", rideSimulation.getRide().getEbike().getId())
                        .put("state", rideSimulation.getRide().getEbike().getState().toString()));
                mapCommunicationAdapter.notifyEndRide(
                    rideSimulation.getRide().getEbike().getId(), userId);
                rideRepository.removeRide(rideSimulation.getRide());
              }
              return CompletableFuture.completedFuture(null);
            });
  }
}
