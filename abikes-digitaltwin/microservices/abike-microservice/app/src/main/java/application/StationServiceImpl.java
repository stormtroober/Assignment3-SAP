package application;

import application.ports.StationCommunicationPort;
import application.ports.StationRepository;
import application.ports.StationServiceAPI;
import domain.model.Station;
import domain.model.StationFactory;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class StationServiceImpl implements StationServiceAPI {

  private final StationRepository repository;
  private final StationCommunicationPort stationCommunicationPort;
  private final AtomicInteger createCounter = new AtomicInteger();

  public StationServiceImpl(StationRepository repository, StationCommunicationPort stationCommunicationPort) {
    this.repository = repository;
    this.stationCommunicationPort = stationCommunicationPort;
    repository.findAll().thenAccept(stationCommunicationPort::sendAllUpdates);
  }

  @Override
  public CompletableFuture<Station> createStation(String id) {
    int count = createCounter.incrementAndGet();
    Station station =
        (count % 2 != 0)
            ? StationFactory.createStandardStation(id)
            : StationFactory.createAlternativeStation(id);

    stationCommunicationPort.sendUpdate(station);
    return repository.save(station).thenApply(v -> station);
  }

  @Override
  public CompletableFuture<Station> updateStation(Station station) {
    return repository
        .update(station)
        .thenCompose(
            v ->
                repository
                    .findById(station.getId())
                    .thenApply(
                        updatedStation -> {
                          stationCommunicationPort.sendUpdate(station);
                          return station;
                        }));
  }

  @Override
  public CompletableFuture<List<Station>> getAllStations() {
    return repository.findAll();
  }

    @Override
    public CompletableFuture<Station> assignBikeToStation(String stationId, String bikeId) {
        return repository
                .findById(stationId)
                .thenCompose(optionalStation -> {
                    if (optionalStation.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("Station not found: " + stationId));
                    }
                    Station station = optionalStation.get();
                    Optional<domain.model.Slot> freeSlot = station.occupyFreeSlot(bikeId);
                    if (freeSlot.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new RuntimeException("No free slots in station: " + stationId));
                    }
                    return updateStation(station);
                });
    }

    @Override
    public CompletableFuture<Station> deassignBikeFromStation(String bikeId) {
        return repository
                .findAll()
                .thenCompose(stations -> {
                    for (Station station : stations) {
                        boolean freed = station.freeSlotByAbike(bikeId);
                        if (freed) {
                            return updateStation(station);
                        }
                    }
                    return CompletableFuture.completedFuture(null);
                });
    }


    public CompletableFuture<Optional<Station>> findStationWithFreeSlot() {
        return getAllStations()
                .thenApply(stations -> stations.stream()
                        .filter(station -> station.getSlots().stream().anyMatch(slot -> !slot.isOccupied()))
                        .findFirst()
                );
    }
}
