package domain.model;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.stream.Collectors;

import static domain.model.StationFactory.MAX_SLOTS;

public class StationMapper {
    public static JsonObject toJson(Station station) {
        JsonArray slots = new JsonArray(
            station.getSlots().stream()
                .map(slot -> new JsonObject()
                    .put("id", slot.getId())
                    .put("abikeId", slot.getAbikeId()))
                .collect(Collectors.toList())
        );
        JsonObject location = new JsonObject()
            .put("x", station.getPosition().getX())
            .put("y", station.getPosition().getY());
        return new JsonObject()
            .put("id", station.getId())
            .put("location", location)
            .put("slots", slots)
            .put("maxSlots", MAX_SLOTS);
    }
}