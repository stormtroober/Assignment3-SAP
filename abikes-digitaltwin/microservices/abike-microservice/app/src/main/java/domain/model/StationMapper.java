package domain.model;

import static domain.model.StationFactory.MAX_SLOTS;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.stream.Collectors;

public class StationMapper {
  public static JsonObject toJson(Station station) {
    JsonArray slots =
        new JsonArray(
            station.getSlots().stream()
                .map(
                    slot ->
                        new JsonObject().put("id", slot.getId()).put("abikeId", slot.getAbikeId()))
                .collect(Collectors.toList()));
    JsonObject location =
        new JsonObject()
            .put("x", station.getPosition().getX())
            .put("y", station.getPosition().getY());
    return new JsonObject()
        .put("id", station.getId())
        .put("location", location)
        .put("slots", slots)
        .put("maxSlots", MAX_SLOTS);
  }

  public static Station fromJson(JsonObject json) {
    String id = json.getString("id");
    JsonObject location = json.getJsonObject("location");
    double x = location.getDouble("x");
    double y = location.getDouble("y");
    P2d position = new P2d(x, y);

    JsonArray slotsArray = json.getJsonArray("slots", new JsonArray());
    List<Slot> slots = new java.util.ArrayList<>();
    for (int i = 0; i < slotsArray.size(); i++) {
      JsonObject slotJson = slotsArray.getJsonObject(i);
      String slotId = slotJson.getString("id");
      String abikeId = slotJson.getString("abikeId");
      Slot slot = new Slot(slotId);
      if (abikeId != null) {
        slot.occupy(abikeId);
      }
      slots.add(slot);
    }

    return new Station(id, position, slots);
  }

}
