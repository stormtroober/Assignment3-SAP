package domain.model;

import ddd.Aggregate;
import java.util.Date;
import java.util.Optional;

public class Ride implements Aggregate<String> {
  private final String id;
  private final User user;
  private final EBike ebike;
  private final Date startTime;
  private volatile Optional<Date> endTime;
  private volatile boolean ongoing;

  public Ride(String id, User user, EBike ebike) {
    this.id = id;
    this.user = user;
    this.ebike = ebike;
    this.startTime = new Date();
    this.endTime = Optional.empty();
    this.ongoing = false;
  }

  @Override
  public String getId() {
    return id;
  }

  public User getUser() {
    return user;
  }

  public EBike getEbike() {
    return ebike;
  }

  public boolean isOngoing() {
    return ongoing;
  }

  public void start() {
    this.ongoing = true;
    this.ebike.setState(EBikeState.IN_USE);
  }

  public void end() {
    if (this.ongoing) {
      this.endTime = Optional.of(new Date());
      this.ongoing = false;
    }
  }

  @Override
  public String toString() {
    return String.format(
        "Ride{id='%s', user='%s', ebike='%s', ebikeState='%s', position='%s', batteryLevel=%d, ongoing=%s}",
        id,
        user.getId(),
        ebike.getId(),
        ebike.getState(),
        ebike.getLocation().toString(),
        ebike.getBatteryLevel(),
        ongoing);
  }
}
