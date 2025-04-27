package domain.model.repository;

import domain.model.Station;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Port representing the repository for managing stations. */
public interface StationRepository {

  /**
   * Saves a station to the repository.
   *
   * @param station the station to save.
   * @return a CompletableFuture that completes when the station is saved.
   */
  CompletableFuture<Void> saveStation(Station station);

  /**
   * Retrieves a station from the repository by its id.
   *
   * @param stationId the id of the station to retrieve.
   * @return a CompletableFuture containing the retrieved station.
   */
  CompletableFuture<Station> getStation(String stationId);

  /**
   * Retrieves a list of all stations.
   *
   * @return a CompletableFuture containing a list of all stations.
   */
  CompletableFuture<List<Station>> getAllStations();
}