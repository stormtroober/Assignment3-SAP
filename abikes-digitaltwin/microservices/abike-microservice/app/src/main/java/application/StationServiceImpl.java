package application;

import application.ports.StationRepository;
import application.ports.StationServiceAPI;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class StationServiceImpl implements StationServiceAPI {

  private final StationRepository repository;

  public StationServiceImpl(StationRepository repository) {
    this.repository = repository;
  }

  @Override
  public CompletableFuture<JsonObject> createStation(String id, float x, float y) {
    JsonObject station =
        new JsonObject().put("id", id).put("location", new JsonObject().put("x", x).put("y", y));
    return repository.save(station).thenApply(v -> station);
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
                    .thenApply(updatedStation -> updatedStation.orElse(station)));
  }

  @Override
  public CompletableFuture<JsonArray> getAllStations() {
    return repository.findAll();
  }
}
