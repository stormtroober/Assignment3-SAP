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
    communicationPort.sendUpdate(station);
    return repository
        .update(station)
        .thenCompose(
            v ->
                repository
                    .findById(station.getString("id"))
                    .thenApply(updatedStation -> updatedStation.orElse(station)));
  }

  @Override
  public CompletableFuture<JsonArray> getAllStations() {
    return repository.findAll();
  }
}