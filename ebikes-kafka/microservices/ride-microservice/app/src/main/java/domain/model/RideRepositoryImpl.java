package domain.model;

import application.ports.EventPublisher;
import ddd.Repository;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ConcurrentHashSet;

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
    public void addRide(Ride ride) {
        RideSimulation sim = new RideSimulation(ride, vertx, publisher);
        rides.add(sim);
    }

    @Override
    public void removeRide(Ride ride) {
        rides.removeIf(sim -> sim.getRide().getId().equals(ride.getId()));
    }

    @Override
    public Ride getRide(String rideId) {
        return rides.stream().filter(sim -> sim.getRide().getId().equals(rideId))
                .findFirst().map(RideSimulation::getRide).orElse(null);
    }

    @Override
    public RideSimulation getRideSimulation(String rideId) {
        return rides.stream().filter(sim -> sim.getRide().getId().equals(rideId))
                .findFirst().orElse(null);
    }

    @Override
    public RideSimulation getRideSimulationByUserId(String userId) {
        return rides.stream().filter(sim -> sim.getRide().getUser().getId().equals(userId))
                .findFirst().orElse(null);
    }
}
