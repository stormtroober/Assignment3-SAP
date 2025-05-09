package domain.model;

public interface Bike {
  String getId();

  int getBatteryLevel();

  P2d getLocation();

  BikeState getState();

  void setState(BikeState state);

  void startRide();
}
