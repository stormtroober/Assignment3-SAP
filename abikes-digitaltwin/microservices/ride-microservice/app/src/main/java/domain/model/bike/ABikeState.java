package domain.model.bike;

public enum ABikeState implements BikeState {
  AVAILABLE,
  IN_USE,
  MAINTENANCE,
  MOVING_TO_USER,
  MOVING_TO_STATION
}
