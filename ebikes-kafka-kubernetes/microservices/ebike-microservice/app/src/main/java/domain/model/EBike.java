package domain.model;

import ddd.Aggregate;
import java.io.Serializable;

public class EBike implements Aggregate<String>, Serializable {

  private final String id;
  private final EBikeState state;
  private final P2d location;
  private final int batteryLevel; // 0..100

  public EBike(String id, P2d location, EBikeState state, int battery) {
    this.id = id;
    this.state = state;
    this.location = location;
    this.batteryLevel = battery;
  }

  public String getId() {
    return id;
  }

  @Override
  public String toString() {
    return String.format(
        "EBike{id='%s', location=%s, batteryLevel=%d%%, state='%s'}",
        id, location, batteryLevel, state);
  }
}
