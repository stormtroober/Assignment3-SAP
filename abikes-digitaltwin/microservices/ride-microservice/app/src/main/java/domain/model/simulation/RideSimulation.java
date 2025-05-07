package domain.model.simulation;

import domain.model.Ride;

import java.util.concurrent.CompletableFuture;

public interface RideSimulation {
    Ride getRide();
    CompletableFuture<Void> startSimulation();
    void stopSimulation();
    void stopSimulationManually();
    String getId();
}