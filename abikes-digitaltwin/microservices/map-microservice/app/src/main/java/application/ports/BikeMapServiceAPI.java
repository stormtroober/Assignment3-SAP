package application.ports;

import domain.model.EBike;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Port representing the REST API for the map service Application. */
public interface BikeMapServiceAPI {

  /**
   * Updates the information of a single e-bike.
   *
   * @param bike the e-bike to update.
   * @return a CompletableFuture that completes when the update is done.
   */
  CompletableFuture<Void> updateEBike(EBike bike);

  /**
   * Updates the information of multiple e-bikes.
   *
   * @param bikes the list of e-bikes to update.
   * @return a CompletableFuture that completes when the updates are done.
   */
  CompletableFuture<Void> updateEBikes(List<EBike> bikes);

  /**
   * Notifies the start of a ride for a user.
   *
   * @param username the username of the user.
   * @param bikeName the name of the e-bike.
   * @return a CompletableFuture that completes when the notification is done.
   */
  CompletableFuture<Void> notifyStartRide(String username, String bikeName);

  /**
   * Notifies the stop of a ride for a user.
   *
   * @param username the username of the user.
   * @param bikeName the name of the e-bike.
   * @return a CompletableFuture that completes when the notification is done.
   */
  CompletableFuture<Void> notifyStopRide(String username, String bikeName);

  /** Retrieves all e-bikes for admin users. */
  void getAllBikes();

  /**
   * Retrieves all e-bikes to a specific user, a normal user can receive only the e-bikes assigned
   * to him + the available ones.
   *
   * @param username the username of the user.
   */
  void getAllBikes(String username);

  /**
   * Registers a new user.
   *
   * @param username the username of the user to register.
   */
  void registerUser(String username);

  /**
   * Deregisters an existing user.
   *
   * @param username the username of the user to deregister.
   */
  void deregisterUser(String username);
}
