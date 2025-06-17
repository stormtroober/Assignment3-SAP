package domain.model.repository;

import domain.model.P2d;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
  CompletableFuture<Void> save(JsonObject station);

  CompletableFuture<Optional<JsonObject>> findById(String id);

  CompletableFuture<JsonArray> findAll();

  CompletableFuture<Void> update(JsonObject station);

  /**
   * Finds the closest station to the specified bike position.
   *
   * @param bikePosition the current position of the bike
   * @return a CompletableFuture containing the closest station, or empty if no stations exist
   */
  CompletableFuture<Optional<JsonObject>> findClosestStation(P2d bikePosition);
}
