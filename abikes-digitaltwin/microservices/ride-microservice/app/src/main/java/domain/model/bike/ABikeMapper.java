package domain.model;

import domain.model.bike.ABike;
import domain.model.bike.ABikeState;
import domain.model.bike.BikeType;
import io.vertx.core.json.JsonObject;

public class ABikeMapper {
  public static ABike fromJson(JsonObject json) {
    String id = json.getString("id");
    ABikeState state = ABikeState.valueOf(json.getString("state"));
    JsonObject loc = json.getJsonObject("location");
    P2d location = new P2d(loc.getDouble("x"), loc.getDouble("y"));
    int batteryLevel = json.getInteger("batteryLevel");
    // Set type to AUTONOMOUS if not present
    BikeType type =
        json.containsKey("type") ? BikeType.valueOf(json.getString("type")) : BikeType.AUTONOMOUS;

    return new ABike(id, location, state, batteryLevel, type);
  }

  public static JsonObject toJson(ABike abike) {
    JsonObject location =
        new JsonObject().put("x", abike.getLocation().x()).put("y", abike.getLocation().y());
    return new JsonObject()
        .put("id", abike.getId())
        .put("state", abike.getState().name())
        .put("batteryLevel", abike.getBatteryLevel())
        .put("location", location)
        .put("type", abike.getType() != null ? abike.getType().name() : null);
  }
}
