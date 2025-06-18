package application.ports;

import domain.model.ABike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/** Port for sending updates to the map microservice adapter. */
public interface BikeCommunicationPort {

  /**
   * Sends an update for a single e-bike to the map service.
   *
   * @param abike a JsonObject representing the e-bike update.
   */
  void sendUpdate(ABike abike);

  /**
   * Sends updates for all e-bikes to the map service.
   *
   * @param abikes a JsonArray containing updates for multiple e-bikes.
   */
  void sendAllUpdates(List<ABike> abikes);
}
