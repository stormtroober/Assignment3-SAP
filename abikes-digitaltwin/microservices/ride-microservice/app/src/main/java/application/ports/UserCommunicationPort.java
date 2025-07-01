package application.ports;

import domain.model.P2d;
import io.vertx.core.json.JsonObject;

/**
 * Port interface for communicating with the user microservice adapter. Defines operations for
 * sending user updates, managing ride dispatches, and initializing the communication channel.
 */
public interface UserCommunicationPort {

  /**
   * Sends an update for a user.
   *
   * @param user a {@link JsonObject} representing the user update
   */
  void sendUpdate(JsonObject user);

  /**
   * Sends a dispatch request for a ride.
   *
   * @param userId the ID of the user
   * @param bikeId the ID of the bike
   * @param position the {@link P2d} position of the user
   */
  void addDispatch(String userId, String bikeId, P2d position);

  /**
   * Removes a dispatch for a ride.
   *
   * @param userId the ID of the user
   * @param bikeId the ID of the bike
   * @param arrived true if the user has arrived, false otherwise
   */
  void removeDispatch(String userId, String bikeId, boolean arrived);

  /** Initializes the communication port. */
  void init();
}
