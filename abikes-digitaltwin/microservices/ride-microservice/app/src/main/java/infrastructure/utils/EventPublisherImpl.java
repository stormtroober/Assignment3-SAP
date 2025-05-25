package infrastructure.utils;

import application.ports.EventPublisher;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class EventPublisherImpl implements EventPublisher {
  private final Vertx vertx;

  public EventPublisherImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public synchronized void publishEBikeUpdate(
      String id, double x, double y, String state, int batteryLevel) {
    publishUpdate(RIDE_UPDATE_ADDRESS_EBIKE, id, x, y, state, batteryLevel);
  }

  @Override
  public void publishABikeStationUpdate(String bikeId, String stationId) {
    JsonObject message =
        new JsonObject().put("bikeName", bikeId).put("stationId", stationId);
    vertx.eventBus().publish(RIDE_UPDATE_ADDRESS_ABIKE_STATION, message);
  }

  @Override
  public synchronized void publishABikeUpdate(
      String id, double x, double y, String state, int batteryLevel) {
    publishUpdate(RIDE_UPDATE_ADDRESS_ABIKE, id, x, y, state, batteryLevel);
  }

  private synchronized void publishUpdate(
      String address, String id, double x, double y, String state, int batteryLevel) {
    JsonObject message =
        new JsonObject()
            .put("id", id)
            .put("state", state)
            .put("location", new JsonObject().put("x", x).put("y", y))
            .put("batteryLevel", batteryLevel);
    vertx.eventBus().publish(address, message);
  }

  @Override
  public synchronized void publishUserUpdate(String username, int credit) {
    JsonObject message = new JsonObject().put("username", username).put("credit", credit);
    vertx.eventBus().publish(RIDE_UPDATE_ADDRESS_USER, message);
  }
}
