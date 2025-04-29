package domain.model;

import ddd.Factory;

public class ABikeFactory implements Factory {
  private static final ABikeFactory INSTANCE = new ABikeFactory();

  private ABikeFactory() {}

  public static ABikeFactory getInstance() {
    return INSTANCE;
  }

  public ABike create(String id, P2d location, ABikeState state, int batteryLevel, BikeType type) {
    return new ABike(id, location, state, batteryLevel, type);
  }
}