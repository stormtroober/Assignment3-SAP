package application.ports;

import domain.model.EBike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;

/** Port for sending updates to the map microservice adapter. */
public interface BikeCommunicationPort {

  /**
   * Sends an update for a single e-bike to the map service.
   *
   * @param ebike a JsonObject representing the e-bike update.
   */
  void sendUpdate(EBike ebike);

  /**
   * Sends updates for all e-bikes to the map service.
   *
   * @param ebikes a JsonArray containing updates for multiple e-bikes.
   */
  void sendAllUpdates(List<EBike> ebikes);
}
