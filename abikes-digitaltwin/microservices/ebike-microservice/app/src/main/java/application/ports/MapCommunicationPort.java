package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

/** Port for sending updates to the map microservice adapter. */
public interface MapCommunicationPort {

  /**
   * Sends an update for a single e-bike to the map service.
   *
   * @param ebike a JsonObject representing the e-bike update.
   */
  void sendUpdate(JsonObject ebike);

  /**
   * Sends updates for all e-bikes to the map service.
   *
   * @param ebikes a JsonArray containing updates for multiple e-bikes.
   */
  void sendAllUpdates(JsonArray ebikes);
}
