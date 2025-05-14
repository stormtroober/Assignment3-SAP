package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationServiceAPI {
  CompletableFuture<JsonObject> createStation(String id);

  CompletableFuture<Optional<JsonObject>> getStation(String id);

  CompletableFuture<JsonObject> updateStation(JsonObject station);

  CompletableFuture<JsonArray> getAllStations();

  CompletableFuture<JsonObject> assignBikeToStation(String stationId, String bikeId);

  CompletableFuture<JsonObject> deassignBikeFromStation(String bikeId);

  CompletableFuture<Optional<JsonObject>> findStationWithFreeSlot();
}
