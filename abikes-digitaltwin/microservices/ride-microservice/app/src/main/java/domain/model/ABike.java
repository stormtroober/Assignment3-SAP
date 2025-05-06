package domain.model;

import ddd.Aggregate;

import java.io.Serializable;

  public class ABike implements Aggregate<String>, Serializable {

    private final String id;
    private final ABikeState state;
    private final P2d location;
    private final int batteryLevel; // 0..100
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

    public P2d getLocation() {
      return location;
    }

    public int getBatteryLevel() {
      return batteryLevel;
    }

    public ABikeState getABikeState() {
      return state;
    }

    public BikeType getType() {
      return type;
    }

    @Override
    public String toString() {
      return String.format(
          "ABike{id='%s', location=%s, batteryLevel=%d%%, state='%s', type='%s'}",
          id, location, batteryLevel, state, type);
    }
  }