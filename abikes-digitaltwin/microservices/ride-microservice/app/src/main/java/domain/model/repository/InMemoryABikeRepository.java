package domain.model.repository;

import domain.model.bike.ABike;
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
  private final Map<String, ABike> abikes = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(ABike abike) {
    String id = abike.getId();
    abikes.put(id, abike);
    logger.info("ABike saved: {}", abike);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<ABike>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(abikes.get(id)));
  }

  @Override
  public CompletableFuture<Void> update(ABike abike) {
    return save(abike);
  }
}
