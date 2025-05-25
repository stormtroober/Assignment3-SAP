package domain.model.repository;

import domain.model.P2d;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryStationRepository implements StationRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryStationRepository.class);
  private final Map<String, JsonObject> stations = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(JsonObject station) {
    String id = station.getString("id");
    stations.put(id, station);
    logger.info("Station saved: {}", station);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(stations.get(id)));
  }

  @Override
  public CompletableFuture<JsonArray> findAll() {
    JsonArray array = new JsonArray();
    stations.values().forEach(array::add);
    return CompletableFuture.completedFuture(array);
  }

  @Override
  public CompletableFuture<Void> update(JsonObject station) {
    return save(station);
  }

    @Override
    public CompletableFuture<Optional<JsonObject>> findClosestStation(P2d bikePosition) {
        if (stations.isEmpty()) {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        JsonObject closestStation = null;
        double minDistance = Double.MAX_VALUE;

        for (JsonObject station : stations.values()) {
            JsonObject location = station.getJsonObject("location");
            if (location != null) {
                double stationX = location.getDouble("x");
                double stationY = location.getDouble("y");

                // Calculate Euclidean distance
                double distance = Math.sqrt(
                        Math.pow(stationX - bikePosition.x(), 2) +
                                Math.pow(stationY - bikePosition.y(), 2)
                );

                if (distance < minDistance) {
                    minDistance = distance;
                    closestStation = station;
                }
            }
        }

        logger.info("Found closest station at distance {}: {}",
                minDistance,
                closestStation != null ? closestStation.getString("id") : "none");

        return CompletableFuture.completedFuture(Optional.ofNullable(closestStation));
    }
}