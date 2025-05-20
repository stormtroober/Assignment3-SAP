package domain.model.simulation;

import application.ports.EventPublisher;
import ddd.Service;
import domain.model.P2d;
import domain.model.Ride;
import domain.model.User;
import domain.model.V2d;
import domain.model.bike.ABike;
import domain.model.bike.ABikeState;
import io.vertx.core.Vertx;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutonomousRideSimulation implements RideSimulation, Service {
  private static final Logger log = LoggerFactory.getLogger(AutonomousRideSimulation.class);
  private final Ride ride;
  private final Vertx vertx;
  private final EventPublisher publisher;
  private final P2d destination;
  private volatile boolean stopped = false;
  private CompletableFuture<Void> future;
  private static final double SPEED = 1.0; // units per tick
  private static final int BATTERY_DECREASE = 1;
  private static final int CREDIT_DECREASE = 1;
  private static final double ARRIVAL_THRESHOLD = 1.5; // distance to consider arrived
  private volatile boolean manuallyStoppedFlag = false;

  public AutonomousRideSimulation(
      Ride ride, Vertx vertx, EventPublisher publisher, P2d destination) {
    this.ride = ride;
    this.vertx = vertx;
    this.publisher = publisher;
    this.destination = destination;
  }

  @Override
  public Ride getRide() {
    return ride;
  }

  @Override
  public CompletableFuture<Void> startSimulation() {
    log.info("Starting simulation for ride {}", ride.getId());
    future = new CompletableFuture<>();
    ride.start();

    if (!ride.isOngoing()) {
      log.warn("Ride {} not ongoing, completing immediately", ride.getId());
      future.complete(null);
      return future;
    }

    vertx.setPeriodic(
            100,
            timerId -> {
              log.debug("Timer tick for ride {}: stopped={}", ride.getId(), stopped);
              if (stopped) {
                vertx.cancelTimer(timerId);
                // Only call completeSimulation if not manually stopped
                if (!manuallyStoppedFlag) {
                  completeSimulation();
                }
                future.complete(null);
              } else {
                updateMovement();
              }
            });

    return future;
  }

  private void updateMovement() {
    User user = ride.getUser();
    synchronized (ride.getBike()) {
      ABike bike = (ABike) ride.getBike();
      P2d current = bike.getLocation();
      V2d toDest = destination.sub(current);
      double dx = toDest.x();
      double dy = toDest.y();
      double distance = Math.hypot(dx, dy);

      log.debug(
          "Ride {} movement update: current={}, destination={}, distance={}",
          ride.getId(),
          current,
          destination,
          distance);

      // check arrival
      if (distance <= ARRIVAL_THRESHOLD) {
        log.info("Ride {} has arrived at destination {}", ride.getId(), current);
        ride.end();
        stopSimulation();
        scheduleCompletion();
        return;
      }

      // compute normalized direction with slight randomness
      double nx = dx / distance;
      double ny = dy / distance;
      V2d dir = new V2d(nx, ny);
      double randomAngleDeg = (Math.random() - 0.5) * 10; // ±5° variation
      dir = dir.rotate(randomAngleDeg);

      // move bike
      V2d movement = dir.mul(SPEED);
      P2d newPos = current.sum(movement);
      bike.setLocation(newPos);

      bike.decreaseBattery(BATTERY_DECREASE);

      if (bike.getBatteryLevel() <= 0) {
        log.info("Battery depleted for ride {}, at location {}", ride.getId(), newPos);
        ride.end();
        stopSimulation();
        scheduleCompletion();
        return;
      }

      user.decreaseCredit(CREDIT_DECREASE);
      if (user.getCredit() == 0) {
        log.info("User {} has no credit, ending ride {}", user.getId(), ride.getId());
        ride.end();
        stopSimulation();
        bike.setState(ABikeState.AVAILABLE);
        scheduleCompletion();
        return;
      }

      log.debug(
          "Publishing update for bike {}: pos={}, state={}, battery={}",
          bike.getId(),
          newPos,
          bike.getState(),
          bike.getBatteryLevel());

      // publish update
      publisher.publishABikeUpdate(
          bike.getId(), newPos.x(), newPos.y(), bike.getState().toString(), bike.getBatteryLevel());

      publisher.publishUserUpdate(user.getId(), user.getCredit());
    }
  }

  private void scheduleCompletion() {
    log.debug("Scheduling completion for ride {}", ride.getId());
    vertx.runOnContext(v -> completeSimulation());
    future.complete(null);
  }

  private void completeSimulation() {
    ABike bike = (ABike) ride.getBike();
    P2d loc = bike.getLocation();
    bike.setState(ABikeState.IN_USE);
    log.info(
        "Completing simulation for ride {} at final location {}, battery={}",
        ride.getId(),
        loc,
        bike.getBatteryLevel());
    publisher.publishABikeUpdate(
        bike.getId(), loc.x(), loc.y(), bike.getState().toString(), bike.getBatteryLevel());
    publisher.publishUserUpdate(ride.getUser().getId(), ride.getUser().getCredit());
  }

  @Override
  public void stopSimulation() {
    stopped = true;
    log.info("Autonomous simulation stopped for ride {}", ride.getId());
  }

  @Override
  public void stopSimulationManually() {
    log.info("Manually stopping simulation for ride {}", ride.getId());
    ride.end();
    if (ride.getBike().getState() == ABikeState.MOVING_TO_USER) {
      ((ABike) ride.getBike()).setState(ABikeState.AVAILABLE);
    }
    // Set the manually stopped flag
    manuallyStoppedFlag = true;

    // Publish updates directly when manually stopped
    ABike bike = (ABike) ride.getBike();
    P2d loc = bike.getLocation();
    publisher.publishABikeUpdate(
            bike.getId(), loc.x(), loc.y(), bike.getState().toString(), bike.getBatteryLevel());
    publisher.publishUserUpdate(ride.getUser().getId(), ride.getUser().getCredit());

    stopSimulation();
  }

  @Override
  public String getId() {
    return ride.getId();
  }
}
