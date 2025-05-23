package domain.model.bike;

import domain.model.P2d;
import domain.model.V2d;

public interface Bike {
  String getId();

  int getBatteryLevel();

  P2d getLocation();

  BikeState getState();

  void setDirection(V2d direction);

  V2d getDirection();

  void decreaseBattery(int amount);

  void setLocation(P2d location);

  void setState(BikeState state);

  void startRide();

  void startRide(BikeState state);
}
