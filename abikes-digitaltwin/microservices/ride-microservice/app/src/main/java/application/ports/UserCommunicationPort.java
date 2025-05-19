package application.ports;

import domain.model.P2d;
import domain.model.User;
import io.vertx.core.json.JsonObject;

/** Port for communicating with the user microservice adapter. */
public interface UserCommunicationPort {

  /**
   * Sends an update for a user.
   *
   * @param user a JsonObject representing the user update.
   */
  void sendUpdate(JsonObject user);

  /**
   * Sends a dispatch request for ride.
   *
   * @param user a JsonObject representing the user.
   */
  void addDispatch(User user, String bikeId, P2d position);

  // Add this to UserCommunicationPort.java
  void removeDispatch(User user, String bikeId, P2d position);


  /** Initializes the communication port. */
  void init();
}
