package domain.model;

import ddd.Aggregate;

public class EBike implements Bike, Aggregate<String> {

  private final String id;
  private volatile EBikeState state;
  private volatile P2d location;
  private volatile V2d direction;
  private volatile double speed; // Units per simulation tick
  private volatile int batteryLevel; // 0..100

  public EBike(String id, double x, double y, EBikeState state, int battery) {
    this.id = id;
    this.state = state;
    this.location = new P2d(x, y);
    this.direction = new V2d(1, 0); // Initial direction
    this.speed = 0; // Default speed
    this.batteryLevel = battery;
  }

  @Override
  public String getId() {
    return id;
  }

  public synchronized EBikeState getState() {
    return state;
  }

  @Override
  public void setState(BikeState state) {
    this.state = (EBikeState) state;
  }

  public synchronized void setState(EBikeState state) {
    this.state = state;
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

  public synchronized int getBatteryLevel() {
    return batteryLevel;
  }

  public synchronized void decreaseBattery(int amount) {
    this.batteryLevel = Math.max(this.batteryLevel - amount, 0);
    if (this.batteryLevel == 0) {
      this.state = EBikeState.MAINTENANCE;
    }
  }

  @Override
  public synchronized void startRide() {
    setState(EBikeState.IN_USE);
  }


  @Override
  public String toString() {
    return String.format(
        "EBike{id='%s', location=%s, batteryLevel=%d%%, state='%s'}",
        id, location, batteryLevel, state);
  }
}
