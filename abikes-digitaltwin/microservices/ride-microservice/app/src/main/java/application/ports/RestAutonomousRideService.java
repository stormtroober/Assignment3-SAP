package application.ports;

import domain.model.P2d;

import java.util.concurrent.CompletableFuture;

public interface RestAutonomousRideService {

    /**
     * Dispatches an autonomous bike to the user location.
     * @param userId the user requesting the A-Bike
     * @param bikeId the identifier of the bike to dispatch
     * @param userLocation the coordinates of the user's location
     * @return a CompletableFuture that completes when the dispatch is started
     */
    CompletableFuture<Void> dispatchBikeToUser(String userId, String bikeId, P2d userLocation);

}
