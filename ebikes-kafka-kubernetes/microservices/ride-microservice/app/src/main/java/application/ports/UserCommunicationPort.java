package application.ports;

import io.vertx.core.json.JsonObject;

/** Port for communicating with the user microservice adapter. */
public interface UserCommunicationPort {

  /**
   * Sends an update for a user.
   *
   * @param user a JsonObject representing the user update.
   */
  void sendUpdate(JsonObject user);

  /** Initializes the communication port. */
  void init();
}
