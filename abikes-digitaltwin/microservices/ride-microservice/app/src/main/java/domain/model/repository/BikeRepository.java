package domain.model.repository;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BikeRepository {
  CompletableFuture<Void> save(JsonObject bike);

  CompletableFuture<Optional<JsonObject>> findById(String id);

  CompletableFuture<JsonArray> findAll();

  CompletableFuture<Void> update(JsonObject bike);
}
