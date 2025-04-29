package domain.model;

import ddd.Factory;

public class EBikeFactory implements Factory {
  private static final EBikeFactory INSTANCE = new EBikeFactory();

  private EBikeFactory() {}

  public static EBikeFactory getInstance() {
    return INSTANCE;
  }

  public EBike create(String id, P2d location, EBikeState state, int batteryLevel, BikeType type) {
    return new EBike(id, location, state, batteryLevel, type);
  }
}
