package domain.model.simulation;

import application.ports.EventPublisher;
import ddd.Service;
import domain.model.Ride;
import domain.model.bike.ABike;
import domain.model.bike.ABikeState;
import domain.model.bike.EBike;
import domain.model.bike.EBikeState;
import io.vertx.core.Vertx;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simulation that executes multiple ride simulations in sequence.
 * Each simulation is executed when the previous one completes.
 */
public class SequentialRideSimulation implements RideSimulation, Service {
    private static final Logger log = LoggerFactory.getLogger(SequentialRideSimulation.class);

    private final String id;
    private final List<RideSimulation> simulations;
    private final Vertx vertx;
    private final EventPublisher publisher;
    private int currentSimulationIndex = -1;
    private volatile boolean stopped = false;
    private CompletableFuture<Void> future;
    private final Function<Ride, Ride> rideTransformer;

    /**
     * Creates a sequential simulation with the given simulations.
     *
     * @param id The ID for this sequential simulation
     * @param simulations The list of simulations to execute in sequence
     * @param vertx The Vertx instance
     * @param publisher The event publisher
     * @param rideTransformer Function to transform the ride between simulation transitions
     */
    public SequentialRideSimulation(
            String id,
            List<RideSimulation> simulations,
            Vertx vertx,
            EventPublisher publisher,
            Function<Ride, Ride> rideTransformer) {
        this.id = id;
        this.simulations = new ArrayList<>(simulations);
        this.vertx = vertx;
        this.publisher = publisher;
        this.rideTransformer = rideTransformer;
    }

    @Override
    public Ride getRide() {
        if (currentSimulationIndex < 0 || currentSimulationIndex >= simulations.size()) {
            // Return the ride from the first simulation as default
            return simulations.getFirst().getRide();
        }
        return simulations.get(currentSimulationIndex).getRide();
    }

    @Override
    public CompletableFuture<Void> startSimulation() {
        log.info("Starting sequential simulation {}", id);
        future = new CompletableFuture<>();

        if (simulations.isEmpty()) {
            log.warn("No simulations to run in sequential simulation {}", id);
            future.complete(null);
            return future;
        }

        // Start with the first simulation
        startNextSimulation();

        return future;
    }

    private void startNextSimulation() {
        if (stopped) {
            log.info("Sequential simulation {} was stopped, not starting next simulation", id);
            future.complete(null);
            return;
        }

        currentSimulationIndex++;

        if (currentSimulationIndex >= simulations.size()) {
            log.info("All simulations completed in sequential simulation {}", id);
            future.complete(null);
            return;
        }

        RideSimulation currentSim = simulations.get(currentSimulationIndex);
        log.info("Starting simulation {} of {} in sequential simulation {}",
                currentSimulationIndex + 1, simulations.size(), id);

        // If not the first simulation, transform the ride from previous to current
        if (currentSimulationIndex > 0) {
            // Get the previous simulation's ride
            Ride previousRide = simulations.get(currentSimulationIndex - 1).getRide();

            // Transform the ride for the new simulation phase
            if (rideTransformer != null) {
                Ride transformedRide = rideTransformer.apply(previousRide);

                // Update bike states appropriately for transition
                if (transformedRide.getBike() instanceof ABike) {
                    ((ABike) transformedRide.getBike()).setState(ABikeState.IN_USE);
                } else if (transformedRide.getBike() instanceof EBike) {
                    ((EBike) transformedRide.getBike()).setState(EBikeState.IN_USE);
                }

                // Publish update for the transformed bike and user
                publishBikeUpdate(transformedRide);
                publisher.publishUserUpdate(
                        transformedRide.getUser().getId(), transformedRide.getUser().getCredit());
            }
        }

        // Start the current simulation
        currentSim.startSimulation()
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error in simulation {}: {}", currentSimulationIndex, error.getMessage());
                        future.completeExceptionally(error);
                    } else {
                        log.info("Simulation {} completed, moving to next", currentSimulationIndex);
                        // Schedule the next simulation on the event loop
                        vertx.runOnContext(v -> startNextSimulation());
                    }
                });
    }

    // Helper to publish appropriate bike update based on bike type
    private void publishBikeUpdate(Ride ride) {
        if (ride.getBike() instanceof ABike bike) {
            publisher.publishABikeUpdate(
                    bike.getId(),
                    bike.getLocation().x(),
                    bike.getLocation().y(),
                    bike.getState().toString(),
                    bike.getBatteryLevel()
            );
        } else if (ride.getBike() instanceof EBike bike) {
            publisher.publishEBikeUpdate(
                    bike.getId(),
                    bike.getLocation().x(),
                    bike.getLocation().y(),
                    bike.getState().toString(),
                    bike.getBatteryLevel()
            );
        }
    }

    @Override
    public void stopSimulation() {
        if (!stopped) {
            log.info("Stopping sequential simulation {}", id);
            stopped = true;

            // Stop the current simulation if any
            if (currentSimulationIndex >= 0 && currentSimulationIndex < simulations.size()) {
                simulations.get(currentSimulationIndex).stopSimulation();
            }

            // Complete the future if not already completed
            if (!future.isDone()) {
                future.complete(null);
            }
        }
    }

    @Override
    public void stopSimulationManually() {
        log.info("Manually stopping sequential simulation {}", id);

        // Stop the current simulation if any
        if (currentSimulationIndex >= 0 && currentSimulationIndex < simulations.size()) {
            simulations.get(currentSimulationIndex).stopSimulationManually();
        }

        stopped = true;

        // Complete the future if not already completed
        if (!future.isDone()) {
            future.complete(null);
        }
    }

    @Override
    public String getId() {
        return id;
    }
}