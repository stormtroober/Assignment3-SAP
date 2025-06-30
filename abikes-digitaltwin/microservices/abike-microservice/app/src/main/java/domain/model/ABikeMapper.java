package domain.model;

import io.vertx.core.json.JsonObject;

public class ABikeMapper {
    public static ABike fromJson(JsonObject json) {
        String id = json.getString("id");
        ABikeState state = ABikeState.valueOf(json.getString("state"));
        JsonObject loc = json.getJsonObject("location");
        P2d location = new P2d(loc.getDouble("x"), loc.getDouble("y"));
        int batteryLevel = json.getInteger("batteryLevel");
        // Set type to AUTONOMOUS if not present
        BikeType type = json.containsKey("type") ? BikeType.valueOf(json.getString("type")) : BikeType.AUTONOMOUS;

        return new ABike(id, location, state, batteryLevel, type);
    }

    public static JsonObject toJson(ABike abike) {
        JsonObject location = new JsonObject()
                .put("x", abike.getLocation().getX())
                .put("y", abike.getLocation().getY());
        return new JsonObject()
                .put("id", abike.getId())
                .put("state", abike.getABikeState().name())
                .put("batteryLevel", abike.getBatteryLevel())
                .put("location", location)
                .put("type", abike.getType() != null ? abike.getType().name() : null);
    }

    public static ABike fromAvro(domain.events.BikeRideUpdate update) {
        String id = update.getId();
        domain.events.Location avroLoc = update.getLocation();
        P2d location = avroLoc != null
                ? new P2d((float) avroLoc.getX(), (float) avroLoc.getY())
                : null;
        ABikeState state = ABikeState.valueOf(update.getState());
        Integer batteryLevel = update.getBatteryLevel();
        int battery = batteryLevel != null ? batteryLevel : 0;
        BikeType type = BikeType.AUTONOMOUS;
        return new ABike(id, location, state, battery, type);
    }
}