package domain.model.bike;

import ddd.Aggregate;
import domain.model.P2d;
import domain.model.V2d;
import java.io.Serializable;

public class ABike implements Bike, Aggregate<String>, Serializable {

  private final String id;
  private volatile ABikeState state;
  private volatile P2d location;
  private volatile V2d direction;  // Added direction field
  private volatile int batteryLevel; // 0..100
  private final BikeType type;

  public ABike(String id, P2d location, ABikeState state, int battery, BikeType type) {
    this.id = id;
    this.state = state;
    this.location = location;
    this.direction = new V2d(1, 0); // Initial direction
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

  public synchronized V2d getDirection() {
    return direction;
  }

  public synchronized void setDirection(V2d direction) {
    this.direction = direction;
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