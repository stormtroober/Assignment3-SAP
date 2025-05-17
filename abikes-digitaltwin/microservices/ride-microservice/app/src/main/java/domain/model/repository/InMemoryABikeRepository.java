package domain.model.repository;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryABikeRepository implements ABikeRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryABikeRepository.class);
  private final Map<String, JsonObject> abikes = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(JsonObject abike) {
    String id = abike.getString("id");
    abikes.put(id, abike);
    logger.info("ABike saved: {}", abike);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(abikes.get(id)));
  }

  @Override
  public CompletableFuture<JsonArray> findAll() {
    JsonArray array = new JsonArray();
    abikes.values().forEach(array::add);
    return CompletableFuture.completedFuture(array);
  }

  @Override
  public CompletableFuture<Void> update(JsonObject abike) {
    return save(abike);
  }
}
