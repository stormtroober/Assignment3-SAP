package domain.model.repository;

import application.ports.EventPublisher;
import ddd.Repository;
import domain.model.P2d;
import domain.model.Ride;
import domain.model.simulation.AutonomousRideSimulation;
import domain.model.simulation.NormalRideSimulation;
import domain.model.simulation.RideSimulation;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;
import java.util.Optional;

public class RideRepositoryImpl implements RideRepository, Repository {

  private final ConcurrentHashSet<RideSimulation> rides;
  private final Vertx vertx;
  private final EventPublisher publisher;

  public RideRepositoryImpl(Vertx vertx, EventPublisher publisher) {
    this.rides = new ConcurrentHashSet<>();
    this.vertx = vertx;
    this.publisher = publisher;
  }

  @Override
  public void addRide(Ride ride, SimulationType type, Optional<P2d> destination) {
    RideSimulation sim;
    switch (type) {
      case AUTONOMOUS_SIM:
        if (destination.isPresent()) {
          sim = new AutonomousRideSimulation(ride, vertx, publisher, destination.get());
          break;
        } else {
          throw new IllegalArgumentException(
              "Destination must be provided for autonomous simulation");
        }
      case NORMAL_SIM:
      default:
        sim = new NormalRideSimulation(ride, vertx, publisher);
        break;
    }
    rides.add(sim);
  }

  @Override
  public void removeRide(Ride ride) {
    rides.removeIf(sim -> sim.getRide().getId().equals(ride.getId()));
  }

  @Override
  public Ride getRide(String rideId) {
    return rides.stream()
        .filter(sim -> sim.getRide().getId().equals(rideId))
        .findFirst()
        .map(RideSimulation::getRide)
        .orElse(null);
  }

  @Override
  public RideSimulation getRideSimulation(String rideId) {
    return rides.stream()
        .filter(sim -> sim.getRide().getId().equals(rideId))
        .findFirst()
        .orElse(null);
  }

  @Override
  public RideSimulation getRideSimulationByUserId(String userId) {
    return rides.stream()
        .filter(sim -> sim.getRide().getUser().getId().equals(userId))
        .findFirst()
        .orElse(null);
  }
}
