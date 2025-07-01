package application.ports;

import domain.model.Station;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Service API for managing {@link Station} domain operations. */
public interface StationServiceAPI {

  /**
   * Creates a new station with the given ID.
   *
   * @param id the unique identifier for the station
   * @return a CompletableFuture containing the created Station
   */
  CompletableFuture<Station> createStation(String id);

  /**
   * Updates the given station's information.
   *
   * @param station the Station object with updated data
   * @return a CompletableFuture containing the updated Station
   */
  CompletableFuture<Station> updateStation(Station station);

  /**
   * Retrieves all stations.
   *
   * @return a CompletableFuture containing a list of all stations
   */
  CompletableFuture<List<Station>> getAllStations();

  /**
   * Assigns a bike to the specified station.
   *
   * @param stationId the ID of the station
   * @param bikeId the ID of the bike to assign
   * @return a CompletableFuture containing the updated Station
   */
  CompletableFuture<Station> assignBikeToStation(String stationId, String bikeId);

  /**
   * Removes a bike assignment from its current station.
   *
   * @param bikeId the ID of the bike to deassign
   * @return a CompletableFuture containing the updated Station
   */
  CompletableFuture<Station> deassignBikeFromStation(String bikeId);

  /**
   * Finds a station that has at least one free slot.
   *
   * @return a CompletableFuture containing an Optional with a station that has a free slot, or
   *     empty if none found
   */
  CompletableFuture<Optional<Station>> findStationWithFreeSlot();
}
