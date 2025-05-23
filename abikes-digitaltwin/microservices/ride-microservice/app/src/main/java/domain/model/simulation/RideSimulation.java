  package domain.model.simulation;

  import domain.model.Ride;
  import domain.model.bike.BikeState;
  import org.apache.kafka.common.quota.ClientQuotaAlteration;

  import java.util.Optional;
  import java.util.concurrent.CompletableFuture;

  public interface RideSimulation {
    Ride getRide();

    CompletableFuture<Void> startSimulation(Optional<BikeState> startingState);

    void stopSimulation();

    void stopSimulationManually();

    String getId();
  }
