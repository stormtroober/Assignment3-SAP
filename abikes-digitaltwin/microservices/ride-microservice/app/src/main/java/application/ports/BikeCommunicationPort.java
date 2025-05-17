package application.ports;

import io.vertx.core.json.JsonObject;

/** Port for communicating with the ebike microservice adapter. */
public interface BikeCommunicationPort {

  /**
   * Sends an update for a single e-bike.
   *
   * @param ebike a JsonObject representing the e-bike update.
   */
  void sendUpdateEBike(JsonObject ebike);

  void sendUpdateABike(JsonObject abike);

  /** Initializes the communication port. */
  void init();
}
