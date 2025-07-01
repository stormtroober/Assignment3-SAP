package domain.model;

import domain.events.EBikeUpdate;
import domain.events.Location;
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
    return EBikeFactory.getInstance().create(id, location, state, batteryLevel, type);
  }

  public static JsonObject toJson(EBike ebike) {
    return new JsonObject()
        .put("id", ebike.getId())
        .put("state", ebike.getState().name())
        .put("batteryLevel", ebike.getBatteryLevel())
        .put(
            "location",
            new JsonObject()
                .put("x", ebike.getLocation().getX())
                .put("y", ebike.getLocation().getY()))
        .put("type", ebike.getType().name());
  }

  public static EBike fromAvro(domain.events.BikeRideUpdate update) {
    String id = update.getId();
    domain.events.Location avroLoc = update.getLocation();
    P2d location = avroLoc != null ? new P2d((float) avroLoc.getX(), (float) avroLoc.getY()) : null;
    EBikeState state = EBikeState.valueOf(update.getState());
    Integer batteryLevel = update.getBatteryLevel();
    int battery = batteryLevel != null ? batteryLevel : 0;
    BikeType type = BikeType.NORMAL;
    return EBikeFactory.getInstance().create(id, location, state, battery, type);
  }

  public static EBikeUpdate toAvro(EBike ebike) {
    Location location = null;
    if (ebike.getLocation() != null) {
      location =
          Location.newBuilder()
              .setX(ebike.getLocation().getX())
              .setY(ebike.getLocation().getY())
              .build();
    }
    return EBikeUpdate.newBuilder()
        .setId(ebike.getId())
        .setState(ebike.getState().name())
        .setLocation(location)
        .setBatteryLevel(ebike.getBatteryLevel())
        .setType(ebike.getType().name())
        .build();
  }
}
