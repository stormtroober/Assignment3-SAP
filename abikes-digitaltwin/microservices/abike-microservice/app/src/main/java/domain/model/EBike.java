package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class EBike<S extends Enum<S>> implements Aggregate<String>, Serializable {

  private final String id;
  private final S state;
  private final P2d location;
  private final int batteryLevel; // 0..100

  public EBike(String id, P2d location, S state, int battery) {
    this.id = id;
    this.state = state;
    this.location = location;
    this.batteryLevel = battery;
  }

  public String getId() {
    return id;
  }

  public P2d getLocation() {
    return location;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public S getState() {
    return state;
  }

  @Override
  public String toString() {
    return String.format(
        "EBike{id='%s', location=%s, batteryLevel=%d%%, state='%s'}",
        id, location, batteryLevel, state);
  }
}