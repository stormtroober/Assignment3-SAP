package application.ports;

import domain.model.EBike;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Port for the EBike Repository. Provides methods to perform CRUD operations on eBikes. */
public interface EBikeRepository {

  /**
   * Saves a new eBike.
   *
   * @param eBike the eBike to save
   * @return a CompletableFuture that completes when the save operation is done
   */
  CompletableFuture<Void> save(EBike eBike);

  /**
   * Updates an existing eBike.
   *
   * @param eBike the eBike to update
   * @return a CompletableFuture that completes when the update operation is done
   */
  CompletableFuture<Void> update(EBike eBike);

  /**
   * Finds an eBike by its id.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing an Optional with the EBike if found, or
   *     an empty Optional if not found
   */
  CompletableFuture<Optional<EBike>> findById(String id);

  /**
   * Retrieves all eBikes.
   *
   * @return a CompletableFuture containing a List of all EBikes
   */
  CompletableFuture<List<EBike>> findAll();
}