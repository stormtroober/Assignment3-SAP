package application.ports;

import domain.model.Station;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Repository interface for managing {@link Station} entities. */
public interface StationRepository {

  /**
   * Saves a new station entity.
   *
   * @param station the station to save
   * @return a CompletableFuture that completes when the operation is done
   */
  CompletableFuture<Void> save(Station station);

  /**
   * Updates an existing station entity.
   *
   * @param station the station to update
   * @return a CompletableFuture that completes when the operation is done
   */
  CompletableFuture<Void> update(Station station);

  /**
   * Finds a station by its unique identifier.
   *
   * @param id the unique identifier of the station
   * @return a CompletableFuture containing an Optional with the found station, or empty if not
   *     found
   */
  CompletableFuture<Optional<Station>> findById(String id);

  /**
   * Retrieves all stations.
   *
   * @return a CompletableFuture containing a list of all stations
   */
  CompletableFuture<List<Station>> findAll();
}
