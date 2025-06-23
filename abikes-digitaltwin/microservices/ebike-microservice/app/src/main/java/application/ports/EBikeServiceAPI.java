package application.ports;

import domain.model.EBike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Port for the EBike Service API Application. Provides methods to manage the domain. */
public interface EBikeServiceAPI {

  /**
   * Creates a new eBike with the given id and location.
   *
   * @param id the unique identifier of the eBike
   * @param x the x-coordinate of the eBike's location
   * @param y the y-coordinate of the eBike's location
   * @return a CompletableFuture containing the created eBike as a JsonObject
   */
  CompletableFuture<EBike> createEBike(String id, float x, float y);

  /**
   * Retrieves an eBike by its id.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing an Optional with the eBike as a JsonObject if found, or
   *     an empty Optional if not found
   */
  CompletableFuture<Optional<EBike>> getEBike(String id);

  /**
   * Recharges the battery of an eBike to 100% and sets its state to AVAILABLE.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing the updated eBike as a JsonObject
   */
  CompletableFuture<EBike> rechargeEBike(String id);

  CompletableFuture<EBike> updateEBike(EBike ebike);

  /**
   * Retrieves all eBikes.
   *
   * @return a CompletableFuture containing a JsonArray of all eBikes
   */
  CompletableFuture<List<EBike>> getAllEBikes();
}
