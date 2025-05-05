package application.ports;

import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletableFuture;

/** Port for communicating with the ebike microservice adapter. */
public interface BikeCommunicationPort {

  /**
   * Sends an update for a single e-bike.
   *
   * @param ebike a JsonObject representing the e-bike update.
   */
  void sendUpdate(JsonObject ebike);

  /**
   * Retrieves the information of an e-bike by its ID.
   *
   * @param id the ID of the e-bike to retrieve.
   * @return a CompletableFuture containing the e-bike information as a JsonObject.
   */
  CompletableFuture<JsonObject> getBike(String id);

  /** Initializes the communication port. */
  void init();
}
