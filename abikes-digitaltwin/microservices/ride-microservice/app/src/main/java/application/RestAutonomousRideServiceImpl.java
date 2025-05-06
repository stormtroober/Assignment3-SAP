package application;

import application.ports.BikeCommunicationPort;
import application.ports.MapCommunicationPort;
import application.ports.RestAutonomousRideService;
import application.ports.UserCommunicationPort;

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
    public CompletableFuture<Void> dispatchBikeToUser(String userId) {
        // Implementation for dispatching an autonomous bike to the user location
        //mapCommunicationAdapter.notifyUserRideCall();
        return CompletableFuture.completedFuture(null);
    }
}
