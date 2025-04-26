package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  CompletableFuture<JsonObject> createEBike(String id, float x, float y);

  /**
   * Retrieves an eBike by its id.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing an Optional with the eBike as a JsonObject if found, or
   *     an empty Optional if not found
   */
  CompletableFuture<Optional<JsonObject>> getEBike(String id);

  /**
   * Recharges the battery of an eBike to 100% and sets its state to AVAILABLE.
   *
   * @param id the unique identifier of the eBike
   * @return a CompletableFuture containing the updated eBike as a JsonObject
   */
  CompletableFuture<JsonObject> rechargeEBike(String id);

  /**
   * Updates the details of an existing eBike.
   *
   * @param ebike the eBike details to update as a JsonObject
   * @return a CompletableFuture containing the updated eBike as a JsonObject
   */
  CompletableFuture<JsonObject> updateEBike(JsonObject ebike);

  /**
   * Retrieves all eBikes.
   *
   * @return a CompletableFuture containing a JsonArray of all eBikes
   */
  CompletableFuture<JsonArray> getAllEBikes();
}
