package application;

import application.ports.CommunicationPort;
import application.ports.StationRepository;
import application.ports.StationServiceAPI;
import domain.model.Station;
import domain.model.StationFactory;
import domain.model.StationMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StationServiceImpl implements StationServiceAPI {

  private final StationRepository repository;
  private final CommunicationPort communicationPort;

  public StationServiceImpl(StationRepository repository, CommunicationPort communicationPort) {
    this.repository = repository;
    this.communicationPort = communicationPort;
    repository.findAll().thenAccept(communicationPort::sendAllUpdates);
  }

  @Override
  public CompletableFuture<JsonObject> createStation(String id) {
    Station station = StationFactory.createStandardStation(id);
    JsonObject stationJson = StationMapper.toJson(station);
    communicationPort.sendUpdate(stationJson);
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
              v -> repository.findById(station.getString("id"))
                  .thenApply(updatedStation -> {
                      JsonObject result = updatedStation.orElse(station);
                      communicationPort.sendUpdate(result);
                      return result;
                  })
          );
  }

  @Override
  public CompletableFuture<JsonArray> getAllStations() {
    return repository.findAll();
  }

  @Override
  public CompletableFuture<JsonObject> assignBikeToStation(String stationId, String bikeId) {
    return repository.findById(stationId).thenCompose(optionalStation -> {
      if (optionalStation.isEmpty()) {
        return CompletableFuture.failedFuture(new RuntimeException("Station not found: " + stationId));
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
        return CompletableFuture.failedFuture(new RuntimeException("No free slots in station: " + stationId));
      }
      freeSlot.put("abikeId", bikeId);
      return updateStation(station);
    });
  }

  @Override
  public CompletableFuture<JsonObject> deassignBikeFromStation(String stationId, String bikeId) {
    return repository.findById(stationId).thenCompose(optionalStation -> {
      if (optionalStation.isEmpty()) {
        return CompletableFuture.failedFuture(new RuntimeException("Station not found: " + stationId));
      }
      JsonObject station = optionalStation.get();
      JsonArray slots = station.getJsonArray("slots");
      JsonObject bikeSlot = null;
      for (int i = 0; i < slots.size(); i++) {
        JsonObject slot = slots.getJsonObject(i);
        if (bikeId.equals(slot.getString("abikeId"))) {
          bikeSlot = slot;
          break;
        }
      }
      if (bikeSlot == null) {
        return CompletableFuture.failedFuture(new RuntimeException("Bike " + bikeId + " not found in station: " + stationId));
      }
      bikeSlot.putNull("abikeId");
      return updateStation(station);
    });
  }

  public CompletableFuture<Optional<JsonObject>> findStationWithFreeSlot() {
      return getAllStations().thenApply(stations -> {
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