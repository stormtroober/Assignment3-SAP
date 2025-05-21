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
import java.util.function.BiConsumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simulation that executes multiple ride simulations in sequence.
 * Each simulation has an associated completion handler that runs when it finishes.
 */
public class SequentialRideSimulation implements RideSimulation, Service {
    private static final Logger log = LoggerFactory.getLogger(SequentialRideSimulation.class);

    private final String id;
    private final List<SimulationStage> stages;
    private final Vertx vertx;
    private final EventPublisher publisher;
    private int currentStageIndex = -1;
    private volatile boolean stopped = false;
    private CompletableFuture<Void> future;

    /**
     * Represents a stage in the sequential simulation with its simulation and completion handler.
     */
    public static class SimulationStage {
        private final RideSimulation simulation;
        private final BiConsumer<RideSimulation, RideSimulation> completionHandler;

        public SimulationStage(
                RideSimulation simulation,
                BiConsumer<RideSimulation, RideSimulation> completionHandler) {
            this.simulation = simulation;
            this.completionHandler = completionHandler;
        }

        public RideSimulation getSimulation() {
            return simulation;
        }

        public BiConsumer<RideSimulation, RideSimulation> getCompletionHandler() {
            return completionHandler;
        }
    }

    /**
     * Creates a sequential simulation with the given simulation stages.
     *
     * @param id The ID for this sequential simulation
     * @param stages The list of simulation stages to execute in sequence
     * @param vertx The Vertx instance
     * @param publisher The event publisher
     */
    public SequentialRideSimulation(
            String id,
            List<SimulationStage> stages,
            Vertx vertx,
            EventPublisher publisher) {
        this.id = id;
        this.stages = new ArrayList<>(stages);
        this.vertx = vertx;
        this.publisher = publisher;
    }

    /**
     * Builder method to create a sequential simulation with stages.
     */
    public static Builder builder(String id, Vertx vertx, EventPublisher publisher) {
        return new Builder(id, vertx, publisher);
    }

    /**
     * Builder for creating SequentialRideSimulation with stages.
     */
    public static class Builder {
        private final String id;
        private final Vertx vertx;
        private final EventPublisher publisher;
        private final List<SimulationStage> stages = new ArrayList<>();

        private Builder(String id, Vertx vertx, EventPublisher publisher) {
            this.id = id;
            this.vertx = vertx;
            this.publisher = publisher;
        }

        /**
         * Adds a simulation stage to the sequence.
         *
         * @param simulation The simulation to run
         * @param completionHandler Handler that runs when simulation completes
         * @return This builder
         */
        public Builder addStage(
                RideSimulation simulation,
                BiConsumer<RideSimulation, RideSimulation> completionHandler) {
            stages.add(new SimulationStage(simulation, completionHandler));
            return this;
        }

        public SequentialRideSimulation build() {
            return new SequentialRideSimulation(id, stages, vertx, publisher);
        }
    }

    @Override
    public Ride getRide() {
        if (currentStageIndex < 0 || currentStageIndex >= stages.size()) {
            // Return the ride from the first simulation as default
            return stages.getFirst().getSimulation().getRide();
        }
        return stages.get(currentStageIndex).getSimulation().getRide();
    }

    @Override
    public CompletableFuture<Void> startSimulation() {
        log.info("Starting sequential simulation {}", id);
        future = new CompletableFuture<>();

        if (stages.isEmpty()) {
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

        // First, run completion handler for previous simulation if needed
        if (currentStageIndex >= 0) {
            SimulationStage currentStage = stages.get(currentStageIndex);
            SimulationStage nextStage = (currentStageIndex + 1 < stages.size()) ?
                    stages.get(currentStageIndex + 1) : null;

            RideSimulation currentSim = currentStage.getSimulation();
            RideSimulation nextSim = nextStage != null ? nextStage.getSimulation() : null;

            try {
                currentStage.getCompletionHandler().accept(currentSim, nextSim);
                publishRideUpdateAfterTransition(currentSim.getRide());
            } catch (Exception e) {
                log.error("Error in completion handler for stage {}: {}",
                        currentStageIndex, e.getMessage(), e);
            }
        }

        currentStageIndex++;

        if (currentStageIndex >= stages.size()) {
            log.info("All simulations completed in sequential simulation {}", id);
            future.complete(null);
            return;
        }

        SimulationStage currentStage = stages.get(currentStageIndex);
        RideSimulation currentSim = currentStage.getSimulation();

        log.info("Starting simulation {} of {} in sequential simulation {}",
                currentStageIndex + 1, stages.size(), id);

        // Start the current simulation
        currentSim.startSimulation()
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Error in simulation {}: {}", currentStageIndex, error.getMessage());
                        future.completeExceptionally(error);
                    } else {
                        log.info("Simulation {} completed, moving to next", currentStageIndex);
                        // Schedule the next simulation on the event loop
                        vertx.runOnContext(v -> startNextSimulation());
                    }
                });
    }

    // Helper to publish appropriate updates after a transition
    private void publishRideUpdateAfterTransition(Ride ride) {
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

        publisher.publishUserUpdate(
                ride.getUser().getId(),
                ride.getUser().getCredit());
    }

    @Override
    public void stopSimulation() {
        if (!stopped) {
            log.info("Stopping sequential simulation {}", id);
            stopped = true;

            // Stop the current simulation if any
            if (currentStageIndex >= 0 && currentStageIndex < stages.size()) {
                stages.get(currentStageIndex).getSimulation().stopSimulation();
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
        if (currentStageIndex >= 0 && currentStageIndex < stages.size()) {
            stages.get(currentStageIndex).getSimulation().stopSimulationManually();
        }

        stopped = true;

        // Complete the future if not already completed
        if (!future.isDone()) {
            future.complete(null);
        }
    }

    public RideSimulation getCurrentSimulation() {
        if (currentStageIndex >= 0 && currentStageIndex < stages.size()) {
            return stages.get(currentStageIndex).getSimulation();
        }
        return null;
    }

    @Override
    public String getId() {
        return id;
    }
}