package domain.model.repository;

import ddd.Repository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Port defining CRUD and assignment operations for bike entities of type T.
 *
 * @param <T> the bike entity type
 */
public interface GenericBikeRepository<T> extends Repository {

  /**
   * Saves or updates a bike in the repository.
   *
   * @param bike the bike to save
   * @return a future that completes when the operation finishes
   */
  CompletableFuture<Void> saveBike(T bike);

  /**
   * Retrieves a bike by its identifier.
   *
   * @param bikeId unique identifier of the bike
   * @return a future containing the bike or completing exceptionally if not found
   */
  CompletableFuture<T> getBike(String bikeId);

  /**
   * Assigns a bike to a user.
   *
   * @param username the user to assign the bike to
   * @param bike the bike to assign
   * @return a future that completes when assignment finishes
   */
  CompletableFuture<Void> assignBikeToUser(String username, T bike);

  /**
   * Unassigns a bike from a user.
   *
   * @param username the user holding the bike
   * @param bike the bike to unassign
   * @return a future that completes when unassignment finishes
   */
  CompletableFuture<Void> unassignBikeFromUser(String username, T bike);

  /**
   * Retrieves all bikes whose state is AVAILABLE.
   *
   * @return a future with a list of available bikes
   */
  CompletableFuture<List<T>> getAvailableBikes();

  /**
   * Checks if a bike is assigned and returns the assignee key.
   *
   * @param bike the bike to check
   * @return a future with the assignee (username or key) or null if unassigned
   */
  CompletableFuture<String> isBikeAssigned(T bike);

  /**
   * Retrieves a map of assignee keys to their assigned and available bikes.
   *
   * @return a future with a map from assignee to list of bikes
   */
  CompletableFuture<Map<String, List<T>>> getUsersWithAssignedAndAvailableBikes();

  /**
   * Retrieves all bikes in the repository.
   *
   * @return a future with a list of all bikes
   */
  CompletableFuture<List<T>> getAllBikes();

  /**
   * Retrieves all bikes assigned to a specific user.
   *
   * @param username the user whose bikes to retrieve
   * @return a future with a list of bikes for the user
   */
  CompletableFuture<List<T>> getAllBikes(String username);
}
