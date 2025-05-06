package application;

import application.ports.BikeCommunicationPort;
import application.ports.MapCommunicationPort;
import application.ports.RestAutonomousRideService;
import application.ports.UserCommunicationPort;
import domain.model.*;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public class RestAutonomousRideServiceImpl implements RestAutonomousRideService {

    private final BikeCommunicationPort abikeCommunicationAdapter;
    private final MapCommunicationPort mapCommunicationAdapter;
    private final UserCommunicationPort userCommunicationAdapter;

    public RestAutonomousRideServiceImpl(
            BikeCommunicationPort abikeCommunicationAdapter,
            MapCommunicationPort mapCommunicationAdapter,
            UserCommunicationPort userCommunicationAdapter) {
        this.abikeCommunicationAdapter = abikeCommunicationAdapter;
        this.mapCommunicationAdapter = mapCommunicationAdapter;
        this.userCommunicationAdapter = userCommunicationAdapter;
    }

    @Override
    public CompletableFuture<Void> dispatchBikeToUser(String userId, String bikeId, P2d userLocation) {
        CompletableFuture<ABike> ebikeFuture = checkABike(bikeId);
        CompletableFuture<User> userFuture = checkUser(userId);

        return CompletableFuture.allOf(ebikeFuture, userFuture)
                .thenCompose(ignored -> {

                    ABike bike = ebikeFuture.join();
                    User user = userFuture.join();

                    if (bike == null || user == null) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("ABike or User not found"));
                    } else if (bike.getABikeState() != ABikeState.AVAILABLE) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("ABike is not available"));
                    } else if (user.getCredit() == 0) {
                        return CompletableFuture.failedFuture(new RuntimeException("User has no credit"));
                    } else if (bike.getBatteryLevel() == 0) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("ABike has no battery"));
                    }

                    System.out.println("Dispatching bike to user: " + userId + " at location: " + userLocation);

                    Ride ride = new Ride("ride-" + userId + "-" + bikeId, user, bike);

                    mapCommunicationAdapter.notifyUserRideCall(bikeId, userId);

                    JsonObject dispatchMessage = new JsonObject()
                            .put("userId", userId)
                            .put("bikeId", bikeId)
                            .put("location", new JsonObject()
                                    .put("x", userLocation.x())
                                    .put("y", userLocation.y()));
                    return userCommunicationAdapter.sendDispatchToRide(dispatchMessage);
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
                            return ABikeFactory.getInstance().create(
                                    abikeJson.getString("id"),
                                    loc,
                                    state,
                                    batteryLevel,
                                    type
                            );
                        });
    }
}
