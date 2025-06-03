package application.ports;

import io.vertx.core.json.JsonObject;

/** Port for communicating with the ebike microservice adapter. */
public interface EbikeCommunicationPort {

  /**
   * Sends an update for a single e-bike.
   *
   * @param ebike a JsonObject representing the e-bike update.
   */
  void sendUpdate(JsonObject ebike);

  /** Initializes the communication port. */
  void init();
}
