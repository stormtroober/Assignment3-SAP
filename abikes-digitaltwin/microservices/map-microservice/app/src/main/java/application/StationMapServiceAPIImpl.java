package application;

import application.ports.EventPublisher;
import application.ports.StationMapServiceAPI;
import domain.model.Slot;
import domain.model.Station;
import domain.model.repository.StationRepository;
import domain.model.repository.StationRepositoryImpl;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
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
            v ->
                repository
                    .getAllStations()
                    .thenAccept(eventPublisher::publishStationsUpdate));
  }

  @Override
  public CompletableFuture<Void> updateStations(List<Station> stations) {
    return CompletableFuture.allOf(
            stations.stream().map(repository::saveStation).toArray(CompletableFuture[]::new))
        .thenAccept(
            v ->
                repository
                    .getAllStations()
                    .thenAccept(eventPublisher::publishStationsUpdate));
  }

  @Override
  public CompletableFuture<Void> deassignBikeFromStation(String bikeId) {
    return repository.getAllStations()
        .thenCompose(stations -> {
          for (Station station : stations) {
            List<Slot> slots = station.getSlots();
            boolean foundBike = false;

            // Create a new list of slots for the updated station
            List<Slot> updatedSlots = new java.util.ArrayList<>();

            for (Slot slot : slots) {
              if (bikeId.equals(slot.getAbikeId())) {
                // Create a new free slot with the same ID (since Slot is immutable)
                updatedSlots.add(new Slot(slot.getId(), null));
                foundBike = true;
              } else {
                updatedSlots.add(slot);
              }
            }

            if (foundBike) {
              // Create an updated station with the modified slots
              Station updatedStation = new Station(
                  station.getId(),
                  station.getLocation(),
                  updatedSlots,
                  station.getMaxSlots()
              );

              // Save the updated station
              return updateStation(updatedStation);
            }
          }

          // Bike not found in any station
          return CompletableFuture.completedFuture(null);
        });
  }
  @Override
  public void getAllStations() {
    repository.getAllStations().thenAccept(eventPublisher::publishStationsUpdate);
  }
}
