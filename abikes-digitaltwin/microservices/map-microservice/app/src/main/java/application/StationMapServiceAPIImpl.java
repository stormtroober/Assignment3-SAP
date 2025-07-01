package application;

import application.ports.EventPublisher;
import application.ports.StationMapServiceAPI;
import domain.model.Station;
import domain.model.repository.StationRepository;
import domain.model.repository.StationRepositoryImpl;
import java.util.concurrent.CompletableFuture;

public class StationMapServiceAPIImpl implements StationMapServiceAPI {

  private final StationRepository repository;
  private final EventPublisher eventPublisher;

  public StationMapServiceAPIImpl(EventPublisher eventPublisher) {
    this.repository = new StationRepositoryImpl();
    this.eventPublisher = eventPublisher;
  }

  @Override
  public CompletableFuture<Void> updateStation(Station station) {
    return repository
        .saveStation(station)
        .thenAccept(
            v -> repository.getAllStations().thenAccept(eventPublisher::publishStationsUpdate));
  }

  @Override
  public void getAllStations() {
    repository.getAllStations().thenAccept(eventPublisher::publishStationsUpdate);
  }
}
