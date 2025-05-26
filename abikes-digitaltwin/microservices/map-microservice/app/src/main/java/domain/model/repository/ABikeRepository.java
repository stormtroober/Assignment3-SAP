package domain.model.repository;

import domain.model.ABike;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Repository port for ABike entities, extending generic operations and adding public-slot
 * assignment methods.
 */
public interface ABikeRepository extends GenericBikeRepository<ABike> {

  /**
   * Assigns a bike to the public slot.
   *
   * @param bike the bike to assign publicly
   * @return a future that completes when assignment finishes
   */
  CompletableFuture<Void> assignBikeToPublic(ABike bike);

  /**
   * Unassigns a bike from the public slot.
   *
   * @param bike the bike to remove from public
   * @return a future that completes when unassignment finishes
   */
  CompletableFuture<Void> unassignBikeFromPublic(ABike bike);

  /**
   * Retrieves all bikes assigned to the public slot.
   *
   * @return a future with a list of public bikes
   */
  CompletableFuture<List<ABike>> getPublicBikes();
}
