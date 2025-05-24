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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class RideRepositoryImpl implements RideRepository, Repository {

  private static final Logger logger = LoggerFactory.getLogger(RideRepositoryImpl.class);
  private final Map<String, RideSimulation> simulations = new ConcurrentHashMap<>();
  private final Map<String, String> userRideMap = new ConcurrentHashMap<>();
  private final Vertx vertx;
  private final EventPublisher publisher;

  public RideRepositoryImpl(Vertx vertx, EventPublisher publisher) {
    this.vertx = vertx;
    this.publisher = publisher;
  }

  @Override
  public void addNormalRide(Ride ride, SimulationType type, Optional<P2d> destination) {
    RideSimulation sim;

    String rideId = ride.getId();
    String userId = ride.getUser().getId();
    sim = new NormalRideSimulation(ride, vertx, publisher);

    simulations.put(rideId, sim);
    userRideMap.put(userId, rideId);
    logger.info("Added ride {} of type {} for user {}", rideId, type, userId);
  }

  @Override
  public void removeRide(Ride ride) {
    String rideId = ride.getId();
    String userId = ride.getUser().getId();

    simulations.remove(rideId);
    userRideMap.remove(userId);

    logger.info("Removed ride {} for user {}", rideId, userId);
  }

  @Override
  public void addRideWithSimulation(Ride ride, RideSimulation simulation) {
    if(simulation == null) {
      throw new IllegalArgumentException("Simulation cannot be null");
    }
    simulations.put(ride.getId(), simulation);
    String userId = simulation.getRide().getUser().getId();
    userRideMap.put(userId, ride.getId());

    logger.info("Set custom simulation for ride {} and user {}", ride.getId(), userId);
  }

  @Override
  public RideSimulation getRideSimulation(String rideId) {
    return simulations.get(rideId);
  }

  @Override
  public RideSimulation getRideSimulationByUserId(String userId) {
    String rideId = userRideMap.get(userId);
    if (rideId != null) {
      return simulations.get(rideId);
    }
    return null;
  }
}
