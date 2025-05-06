package application.ports;

import io.vertx.core.json.JsonObject;
import java.util.concurrent.CompletableFuture;

/** Port for communicating with the user microservice adapter. */
public interface UserCommunicationPort {

  /**
   * Sends an update for a user.
   *
   * @param user a JsonObject representing the user update.
   */
  void sendUpdate(JsonObject user);

    /**
     * Sends a dispatch request for ride.
     *
     * @param dispatchPayload a JsonObject containing the dispatch information.
     * @return a CompletableFuture indicating the completion of the operation.
     */
  CompletableFuture<Void> sendDispatchToRide(JsonObject dispatchPayload);


  /**
   * Retrieves the information of a user by their ID.
   *
   * @param id the ID of the user to retrieve.
   * @return a CompletableFuture containing the user information as a JsonObject.
   */
  CompletableFuture<JsonObject> getUser(String id);

  /** Initializes the communication port. */
  void init();
}
