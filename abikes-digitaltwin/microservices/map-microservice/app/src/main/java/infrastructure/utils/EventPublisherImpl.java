package infrastructure.utils;

import application.ports.EventPublisher;
import domain.model.EBike;
import domain.model.Station;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.List;

public class EventPublisherImpl implements EventPublisher {
  private final Vertx vertx;

  public EventPublisherImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void publishBikesUpdate(List<EBike> bikes) {
    JsonArray bikesJson = new JsonArray();
    bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
    vertx.eventBus().publish("bikes.update", bikesJson.encode());
  }

  @Override
  public void publishUserBikesUpdate(List<EBike> bikes, String username) {
    JsonArray bikesJson = new JsonArray();
    bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
    vertx.eventBus().publish(username, bikesJson.encode());
  }

  @Override
  public void publishUserAvailableBikesUpdate(List<EBike> bikes) {
    JsonArray bikesJson = new JsonArray();
    bikes.forEach(bike -> bikesJson.add(convertBikeToJson(bike)));
    vertx.eventBus().publish("available_bikes", bikesJson.encode());
  }

  @Override
  public void publishStopRide(String username) {
    JsonObject json = new JsonObject();
    json.put("rideStatus", "stopped");
    vertx.eventBus().publish("ride.stop." + username, json.encode());
  }

  @Override
  public void publishStationsUpdate(List<Station> stations) {
    JsonArray stationsJson = new JsonArray();
    stations.forEach(station -> stationsJson.add(convertStationToJson(station)));
    vertx.eventBus().publish("stations.update", stationsJson.encode());
  }

  private String convertBikeToJson(EBike bike) {
    JsonObject json = new JsonObject();
    json.put("bikeName", bike.getId());
    json.put(
        "position",
        new JsonObject().put("x", bike.getPosition().x()).put("y", bike.getPosition().y()));
    json.put("state", bike.getState().toString());
    json.put("batteryLevel", bike.getBatteryLevel());
    json.put("type", bike.getType().toString());
    return json.encode();
  }

  private String convertStationToJson(Station station) {
      JsonObject json = new JsonObject();
      json.put("id", station.getId());
      json.put(
          "location",
          new JsonObject()
              .put("x", station.getLocation().x())
              .put("y", station.getLocation().y())
      );
      JsonArray slotsJson = new JsonArray();
      station.getSlots().forEach(slot -> {
          JsonObject slotJson = new JsonObject()
              .put("id", slot.getId())
              .put("abikeId", slot.getAbikeId());
          slotsJson.add(slotJson);
      });
      json.put("slots", slotsJson);
      json.put("maxSlots", station.getMaxSlots());
      return json.encode();
  }
}
