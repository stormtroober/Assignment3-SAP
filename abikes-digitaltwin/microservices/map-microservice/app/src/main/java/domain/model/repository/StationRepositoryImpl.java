package domain.model.repository;

import ddd.Repository;
import domain.model.Station;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class StationRepositoryImpl implements StationRepository, Repository {
  private final ConcurrentHashMap<String, Station> stations = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> saveStation(Station station) {
    stations.put(station.getId(), station);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<Station> getStation(String stationId) {
    Station station = stations.get(stationId);
    if (station != null) {
      return CompletableFuture.completedFuture(station);
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Station not found"));
    }
  }

  @Override
  public CompletableFuture<List<Station>> getAllStations() {
    return CompletableFuture.supplyAsync(() -> new ArrayList<>(stations.values()));
  }
}