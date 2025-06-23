package domain.model.repository;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface BikeRepository<T> {
  CompletableFuture<Void> save(T bike);

  CompletableFuture<Optional<T>> findById(String id);

  CompletableFuture<Void> update(T bike);
}
