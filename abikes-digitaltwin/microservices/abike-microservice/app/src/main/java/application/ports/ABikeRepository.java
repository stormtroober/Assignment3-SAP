package application.ports;

import domain.model.ABike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Port for the EBike Repository. Provides methods to perform CRUD operations on eBikes. */
public interface ABikeRepository {

  /**
   * Saves a new eBike.
   *
   * @param aBike the eBike details to save as a JsonObject
   * @return a CompletableFuture that completes when the save operation is done
   */
  CompletableFuture<Void> save(ABike aBike);

  /**
   * Updates an existing eBike.
   *
   * @param aBike the eBike details to update as a JsonObject
   * @return a CompletableFuture that completes when the update operation is done
   */
  CompletableFuture<Void> update(ABike aBike);

  /**
   * Finds an eBike by its id.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing an Optional with the eBike as a JsonObject if found, or
   *     an empty Optional if not found
   */
  CompletableFuture<Optional<ABike>> findById(String id);

  /**
   * Retrieves all eBikes.
   *
   * @return a CompletableFuture containing a JsonArray of all eBikes
   */
  CompletableFuture<List<ABike>> findAll();
}
