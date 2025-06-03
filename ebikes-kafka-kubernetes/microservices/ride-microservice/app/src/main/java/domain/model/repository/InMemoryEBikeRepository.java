package domain.model.repository;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryEBikeRepository implements EBikeRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryEBikeRepository.class);
  private final Map<String, JsonObject> ebikes = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(JsonObject ebike) {
    String id = ebike.getString("id");
    ebikes.put(id, ebike);
    logger.info("EBike saved: {}", ebike);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(ebikes.get(id)));
  }

  @Override
  public CompletableFuture<JsonArray> findAll() {
    JsonArray array = new JsonArray();
    ebikes.values().forEach(array::add);
    return CompletableFuture.completedFuture(array);
  }

  @Override
  public CompletableFuture<Void> update(JsonObject ebike) {
    return save(ebike);
  }
}
