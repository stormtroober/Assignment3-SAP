package domain.model;

import domain.model.bike.BikeType;
import domain.model.bike.EBike;
import domain.model.bike.EBikeState;
import io.vertx.core.json.JsonObject;

public class EBikeMapper {

  public static EBike fromJson(JsonObject json) {
    String id = json.getString("id");
    JsonObject loc = json.getJsonObject("location");
    float x = loc.getFloat("x");
    float y = loc.getFloat("y");
    P2d location = new P2d(x, y);
    EBikeState state = EBikeState.valueOf(json.getString("state"));
    int batteryLevel = json.getInteger("batteryLevel");
    BikeType type =
        json.containsKey("type") ? BikeType.valueOf(json.getString("type")) : BikeType.NORMAL;
    return new EBike(id, location.x(), location.y(), state, batteryLevel);
  }

  public static JsonObject toJson(EBike ebike) {
    return new JsonObject()
        .put("id", ebike.getId())
        .put("state", ebike.getState().name())
        .put("batteryLevel", ebike.getBatteryLevel())
        .put(
            "location",
            new JsonObject()
                .put("x", ebike.getLocation().x())
                .put("y", ebike.getLocation().y())
                .put("type", BikeType.NORMAL));
  }
}
