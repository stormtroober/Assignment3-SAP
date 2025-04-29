package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class EBike implements Aggregate<String>, Serializable {

  private final String id;
  private final EBikeState state;
  private final P2d location;
  private final int batteryLevel; // 0..100
  private final BikeType type;

  public EBike(String id, P2d location, EBikeState state, int battery, BikeType type) {
    this.id = id;
    this.state = state;
    this.location = location;
    this.batteryLevel = battery;
    this.type = type;
  }

  public String getId() {
    return id;
  }

  public EBikeState getState() {
    return state;
  }

  public int getBatteryLevel() {
    return batteryLevel;
  }

  public P2d getPosition() {return location;}

  public BikeType getType() {
    return type;
  }

  @Override
  public String toString() {
    return String.format(
        "EBike{id='%s', location=%s, batteryLevel=%d%%, state='%s', type='%s'}",
        id, location, batteryLevel, state, type);
  }
}
