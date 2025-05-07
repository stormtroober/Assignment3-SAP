package domain.model.bike;

import domain.model.P2d;

public interface Bike {
    String getId();
    int getBatteryLevel();
    P2d getLocation();
    BikeState getState();
    void setState(BikeState state);
    void startRide();
}
