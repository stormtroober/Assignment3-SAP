package domain.model.repository;

import domain.model.P2d;
import domain.model.Station;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
  CompletableFuture<Void> save(Station station);

  CompletableFuture<Optional<Station>> findById(String id);

  CompletableFuture<Void> update(Station station);

  /**
   * Finds the closest station to the specified bike position.
   *
   * @param bikePosition the current position of the bike
   * @return a CompletableFuture containing the closest station, or empty if no stations exist
   */
  CompletableFuture<Optional<Station>> findClosestStation(P2d bikePosition);
}