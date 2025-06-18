package application;

import application.ports.StationCommunicationPort;
import application.ports.StationRepository;
import application.ports.StationServiceAPI;
import domain.model.Station;
import domain.model.StationFactory;
import domain.model.StationMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  public CompletableFuture<JsonObject> createStation(String id) {
    int count = createCounter.incrementAndGet();
    Station station =
        (count % 2 != 0)
            ? StationFactory.createStandardStation(id)
            : StationFactory.createAlternativeStation(id);
    JsonObject stationJson = StationMapper.toJson(station);
    stationCommunicationPort.sendUpdate(stationJson);
    return repository.save(stationJson).thenApply(v -> stationJson);
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> getStation(String id) {
    return repository.findById(id);
  }

  @Override
  public CompletableFuture<JsonObject> updateStation(JsonObject station) {
    return repository
        .update(station)
        .thenCompose(
            v ->
                repository
                    .findById(station.getString("id"))
                    .thenApply(
                        updatedStation -> {
                          JsonObject result = updatedStation.orElse(station);
                          stationCommunicationPort.sendUpdate(result);
                          return result;
                        }));
  }

  @Override
  public CompletableFuture<JsonArray> getAllStations() {
    return repository.findAll();
  }

  @Override
  public CompletableFuture<JsonObject> assignBikeToStation(String stationId, String bikeId) {
    return repository
        .findById(stationId)
        .thenCompose(
            optionalStation -> {
              if (optionalStation.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("Station not found: " + stationId));
              }
              JsonObject station = optionalStation.get();
              JsonArray slots = station.getJsonArray("slots");
              JsonObject freeSlot = null;
              for (int i = 0; i < slots.size(); i++) {
                JsonObject slot = slots.getJsonObject(i);
                if (slot.getString("abikeId") == null) {
                  freeSlot = slot;
                  break;
                }
              }
              if (freeSlot == null) {
                return CompletableFuture.failedFuture(
                    new RuntimeException("No free slots in station: " + stationId));
              }
              freeSlot.put("abikeId", bikeId);
              return updateStation(station);
            });
  }

  @Override
  public CompletableFuture<JsonObject> deassignBikeFromStation(String bikeId) {
    return repository
        .findAll()
        .thenCompose(
            stations -> {
              for (int i = 0; i < stations.size(); i++) {
                JsonObject station = stations.getJsonObject(i);
                JsonArray slots = station.getJsonArray("slots");
                boolean foundBike = false;

                for (int j = 0; j < slots.size(); j++) {
                  JsonObject slot = slots.getJsonObject(j);
                  if (bikeId.equals(slot.getString("abikeId"))) {
                    // Set the abikeId to null in this slot
                    slot.putNull("abikeId");
                    foundBike = true;
                  }
                }

                if (foundBike) {
                  // Update the station with the modified slots
                  return updateStation(station);
                }
              }

              // Bike not found in any station
              return CompletableFuture.completedFuture(null);
            });
  }

  public CompletableFuture<Optional<JsonObject>> findStationWithFreeSlot() {
    return getAllStations()
        .thenApply(
            stations -> {
              for (int i = 0; i < stations.size(); i++) {
                JsonObject station = stations.getJsonObject(i);
                JsonArray slots = station.getJsonArray("slots");
                for (int j = 0; j < slots.size(); j++) {
                  JsonObject slot = slots.getJsonObject(j);
                  if (slot.getString("abikeId") == null) {
                    return Optional.of(station);
                  }
                }
              }
              return Optional.empty();
            });
  }
}
