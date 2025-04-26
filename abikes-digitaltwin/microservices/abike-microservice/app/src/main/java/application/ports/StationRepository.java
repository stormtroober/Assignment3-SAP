package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
  CompletableFuture<Void> save(JsonObject station);

  CompletableFuture<Void> update(JsonObject station);

  CompletableFuture<Optional<JsonObject>> findById(String id);

  CompletableFuture<JsonArray> findAll();
}
