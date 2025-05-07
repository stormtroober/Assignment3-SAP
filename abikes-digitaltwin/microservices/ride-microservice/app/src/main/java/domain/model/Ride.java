package domain.model;

import ddd.Aggregate;
import domain.model.bike.Bike;

import java.util.Date;
import java.util.Optional;

public class Ride implements Aggregate<String> {
  private final String id;
  private final User user;
  private final Bike bike;
  private final Date startTime;
  private volatile Optional<Date> endTime;
  private volatile boolean ongoing;

  public Ride(String id, User user, Bike bike) {
    this.id = id;
    this.user = user;
    this.bike = bike;
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

  public boolean isOngoing() {
    return ongoing;
  }

  public void start() {
    this.ongoing = true;
    this.bike.startRide();
  }

  public void end() {
    if (this.ongoing) {
      this.endTime = Optional.of(new Date());
      this.ongoing = false;
    }
  }

  public Bike getBike() {
    return bike;
  }

  @Override
  public String toString() {
    return String.format(
            "Ride{id='%s', user='%s', bike='%s', bikeState='%s', position='%s', batteryLevel=%d, ongoing=%s}",
            id,
            user.getId(),
            bike.getId(),
            bike.getState().name(),
            bike.getLocation().toString(),
            bike.getBatteryLevel(),
            ongoing);
  }

}
