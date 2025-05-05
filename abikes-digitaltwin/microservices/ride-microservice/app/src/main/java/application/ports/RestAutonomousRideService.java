package application.ports;

import java.util.concurrent.CompletableFuture;

public interface RestAutonomousRideService {

    /**
     * Dispatches an autonomous bike to the user location.
     * @param userId the user requesting the A-Bike
     * @return a CompletableFuture that completes when the dispatch is started
     */
    CompletableFuture<Void> dispatchBikeToUser(String userId);

}
