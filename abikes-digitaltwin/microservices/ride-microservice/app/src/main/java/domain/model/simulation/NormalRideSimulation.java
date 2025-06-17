package domain.model.simulation;

import application.ports.EventPublisher;
import ddd.Service;
import domain.model.Ride;
import domain.model.User;
import domain.model.V2d;
import domain.model.bike.*;
import io.vertx.core.Vertx;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class NormalRideSimulation implements RideSimulation, Service {
  private final Ride ride;
  private final Vertx vertx;
  private volatile boolean stopped = false;
  private long lastTimeChangedDir = System.currentTimeMillis();
  private final EventPublisher publisher;
  private static final int CREDIT_DECREASE = 1;
  private static final int BATTERY_DECREASE = 1;
  private final String id;

  public NormalRideSimulation(Ride ride, Vertx vertx, EventPublisher publisher) {
    this.ride = ride;
    this.vertx = vertx;
    this.publisher = publisher;
    this.id = ride.getId();
  }

  public Ride getRide() {
    return ride;
  }

  public CompletableFuture<Void> startSimulation(Optional<BikeState> startingState) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (startingState.isPresent()) {
      ride.start((ABikeState) startingState.get());
    } else {
      ride.start();
    }

    if (ride.isOngoing()) {
      vertx.setPeriodic(
          100,
          timerId -> {
            if (stopped) {
              vertx.cancelTimer(timerId);
              future.complete(null);
              completeSimulation();
              return;
            }

            updateRide();
          });
    } else {
      future.complete(null);
    }

    return future;
  }

  private void updateRide() {
    User user = ride.getUser();

    synchronized (ride.getBike()) {
      Bike bike = ride.getBike();
      if (bike.getBatteryLevel() == 0) {
        System.out.println("Bike has no battery");
        ride.end();
        stopSimulation();
        completeSimulation();
      }

      if (user.getCredit() == 0) {
        ride.end();
        stopSimulation();
        if (bike instanceof EBike) {
          bike.setState(EBikeState.AVAILABLE);
        } else {
          bike.setState(ABikeState.AVAILABLE);
        }
        completeSimulation();
      }

      V2d direction = bike.getDirection();
      double speed = 0.5; // Set speed to a constant value for simplicity
      V2d movement = direction.mul(speed);
      bike.setLocation(bike.getLocation().sum(movement));

      if (bike.getLocation().x() > 200 || bike.getLocation().x() < -200) {
        bike.setDirection(new V2d(-direction.x(), direction.y()));
      }
      if (bike.getLocation().y() > 200 || bike.getLocation().y() < -200) {
        bike.setDirection(new V2d(direction.x(), -direction.y()));
      }

      long elapsedTimeSinceLastChangeDir = System.currentTimeMillis() - lastTimeChangedDir;
      if (elapsedTimeSinceLastChangeDir > 500) {
        double angle = Math.random() * 60 - 30;
        bike.setDirection(direction.rotate(angle));
        lastTimeChangedDir = System.currentTimeMillis();
      }

      bike.decreaseBattery(BATTERY_DECREASE);
      user.decreaseCredit(CREDIT_DECREASE);

      publishBikeUpdate(bike);

      publisher.publishUserUpdate(user.getId(), user.getCredit());
    }
  }

  private void completeSimulation() {
    publishBikeUpdate(ride.getBike());
    publisher.publishUserUpdate(ride.getUser().getId(), ride.getUser().getCredit());
  }

  public void stopSimulation() {
    System.out.println("Stopping simulation " + stopped);
    stopped = true;
  }

  public void stopSimulationManually() {
    System.out.println("Stopping simulation manually");
    ride.end();
    if (ride.getBike() instanceof EBike) {
      ride.getBike().setState(EBikeState.AVAILABLE);
    } else if (ride.getBike() instanceof ABike) {
      ride.getBike().setState(ABikeState.AVAILABLE);
    }
    stopped = true;
  }

  public String getId() {
    return id;
  }

  private void publishBikeUpdate(Bike bike) {
    if (bike instanceof EBike) {
      publisher.publishEBikeUpdate(
          bike.getId(),
          bike.getLocation().x(),
          bike.getLocation().y(),
          bike.getState().toString(),
          bike.getBatteryLevel());
    } else if (bike instanceof ABike) {
      publisher.publishABikeUpdate(
          bike.getId(),
          bike.getLocation().x(),
          bike.getLocation().y(),
          bike.getState().toString(),
          bike.getBatteryLevel());
      ;
    }
  }
}
