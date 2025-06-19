package application.ports;

import domain.model.Station;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.kafka.common.metrics.Stat;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationServiceAPI {
  CompletableFuture<Station> createStation(String id);

  CompletableFuture<Station> updateStation(Station station);

  CompletableFuture<List<Station>> getAllStations();

  CompletableFuture<Station> assignBikeToStation(String stationId, String bikeId);

  CompletableFuture<Station> deassignBikeFromStation(String bikeId);

  CompletableFuture<Optional<JsonObject>> findStationWithFreeSlot();
}
