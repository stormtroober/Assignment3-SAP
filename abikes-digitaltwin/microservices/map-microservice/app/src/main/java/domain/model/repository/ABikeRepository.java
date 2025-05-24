package domain.model.repository;

import domain.model.ABike;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface ABikeRepository extends GenericBikeRepository<ABike> {
  // This interface extends GenericBikeRepository with the specific type ABike.
  CompletableFuture<Void> assignBikeToPublic(ABike bike);
  CompletableFuture<Void> unassignBikeFromPublic(ABike bike);
  CompletableFuture<List<ABike>> getPublicBikes();
}
