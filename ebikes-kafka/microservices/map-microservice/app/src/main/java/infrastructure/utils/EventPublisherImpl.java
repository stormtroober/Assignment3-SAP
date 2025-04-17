package infrastructure.utils;

import application.ports.EventPublisher;
import domain.model.EBike;
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
        vertx.eventBus().publish("ride.stop."+username , json.encode());
    }

    private String convertBikeToJson(EBike bike) {
        JsonObject json = new JsonObject();
        json.put("bikeName", bike.getBikeName());
        json.put("position", new JsonObject()
                .put("x", bike.getPosition().x())
                .put("y", bike.getPosition().y()));
        json.put("state", bike.getState().toString());
        json.put("batteryLevel", bike.getBatteryLevel()); // Add this line
        return json.encode();
    }

}