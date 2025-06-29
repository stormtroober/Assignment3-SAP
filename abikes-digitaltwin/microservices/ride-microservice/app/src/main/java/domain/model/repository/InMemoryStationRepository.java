package domain.model.repository;

import domain.model.P2d;
import domain.model.Station;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryStationRepository implements StationRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryStationRepository.class);
  private final Map<String, Station> stations = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> save(Station station) {
    stations.put(station.getId(), station);
    logger.info("Station saved: {}", station.getId());
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Optional<Station>> findById(String id) {
    return CompletableFuture.completedFuture(Optional.ofNullable(stations.get(id)));
  }

  @Override
  public CompletableFuture<Void> update(Station station) {
    return save(station);
  }

  @Override
  public CompletableFuture<Optional<Station>> findClosestStation(P2d bikePosition) {
    if (stations.isEmpty()) {
      return CompletableFuture.completedFuture(Optional.empty());
    }

    Station closestStation = null;
    double minDistance = Double.MAX_VALUE;

    for (Station station : stations.values()) {
      P2d location = station.getLocation();
      double distance = Math.sqrt(
              Math.pow(location.x() - bikePosition.x(), 2) +
                      Math.pow(location.y() - bikePosition.y(), 2));

      if (distance < minDistance) {
        minDistance = distance;
        closestStation = station;
      }
    }

    logger.info("Found closest station at distance {}: {}",
            minDistance, closestStation != null ? closestStation.getId() : "none");

    return CompletableFuture.completedFuture(Optional.ofNullable(closestStation));
  }
}