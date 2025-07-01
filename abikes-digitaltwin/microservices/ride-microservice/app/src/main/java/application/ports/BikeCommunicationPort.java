package application.ports;

import io.vertx.core.json.JsonObject;

/**
 * Port interface for communicating with the e-bike microservice adapter. Defines operations for
 * sending updates about bikes and initializing the communication channel.
 */
public interface BikeCommunicationPort {

  /**
   * Sends an update for a single e-bike.
   *
   * @param ebike a {@link JsonObject} representing the e-bike update
   */
  void sendUpdateEBike(JsonObject ebike);

  /**
   * Sends an update for a single a-bike.
   *
   * @param abike a {@link JsonObject} representing the a-bike update
   */
  void sendUpdateABike(JsonObject abike);

  /** Initializes the communication port. */
  void init();
}
