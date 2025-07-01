package application.ports;

import domain.model.Station;
import java.util.concurrent.CompletableFuture;

/** Service API for managing station mapping operations. */
public interface StationMapServiceAPI {

  /**
   * Updates the information of a given station.
   *
   * @param station the {@link Station} object with updated data
   * @return a {@link CompletableFuture} that completes when the update is done
   */
  CompletableFuture<Void> updateStation(Station station);

  /** Retrieves all stations. */
  void getAllStations();
}
