package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class ABike implements Bike, Aggregate<String>, Serializable {

  private final String id;
  private volatile ABikeState state;
  private volatile P2d location;
  private volatile int batteryLevel; // 0..100
  private final BikeType type;

  public ABike(String id, P2d location, ABikeState state, int battery, BikeType type) {
    this.id = id;
    this.state = state;
    this.location = location;
    this.batteryLevel = battery;
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public synchronized P2d getLocation() {
    return location;
  }

  public synchronized void setLocation(P2d location) {
    this.location = location;
  }

  @Override
  public synchronized BikeState getState() {
    return state;
  }

  @Override
  public void setState(BikeState state) {
    this.state = (ABikeState) state;
  }

  public synchronized void setState(ABikeState state) {
    this.state = state;
  }

  public synchronized int getBatteryLevel() {
    return batteryLevel;
  }

  public synchronized void decreaseBattery(int amount) {
    this.batteryLevel = Math.max(this.batteryLevel - amount, 0);
    if (this.batteryLevel == 0) {
      this.state = ABikeState.MAINTENANCE;
    }
  }

  public BikeType getType() {
    return type;
  }

  @Override
  public void startRide() {
    setState(ABikeState.MOVING_TO_USER);
  }

  @Override
  public synchronized String toString() {
    return String.format(
        "ABike{id='%s', location=%s, batteryLevel=%d%%, state='%s', type='%s'}",
        id, location, batteryLevel, state, type);
  }
}
