package domain.model.repository;

import domain.model.bike.EBike;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryEBikeRepository implements EBikeRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryEBikeRepository.class);
  private final Map<String, EBike> ebikes = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(EBike ebike) {
    String id = ebike.getId();
    ebikes.put(id, ebike);
    logger.info("EBike saved: {}", ebike);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<EBike>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(ebikes.get(id)));
  }

  @Override
  public CompletableFuture<Void> update(EBike ebike) {
    return save(ebike);
  }
}
