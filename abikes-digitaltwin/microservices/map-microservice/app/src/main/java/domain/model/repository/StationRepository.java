package domain.model.repository;

import domain.model.Station;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Port representing the repository for managing stations. */
public interface StationRepository {

  /**
   * Saves a station to the repository.
   *
   * @param station the station to save
   * @return a future that completes when the station is stored
   */
  CompletableFuture<Void> saveStation(Station station);

  /**
   * Retrieves a station by its identifier.
   *
   * @param stationId unique identifier of the station
   * @return a future containing the station or completing exceptionally if not found
   */
  CompletableFuture<Station> getStation(String stationId);

  /**
   * Retrieves all stations.
   *
   * @return a future with a list of all stations
   */
  CompletableFuture<List<Station>> getAllStations();
}
