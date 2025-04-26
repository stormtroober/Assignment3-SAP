package domain.model;

public class ABike extends EBike<ABikeState> {

  public ABike(String id, P2d location, ABikeState state, int battery) {
    super(id, location, state, battery);
  }

  public ABikeState getABikeState() {
    return getState();
  }

  @Override
  public String toString() {
    return String.format(
        "ABike{id='%s', location=%s, batteryLevel=%d%%, state='%s'}",
        getId(), getLocation(), getBatteryLevel(), getState());
  }
}