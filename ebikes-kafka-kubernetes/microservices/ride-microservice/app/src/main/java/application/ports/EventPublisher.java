package application.ports;

/** Port representing an event publisher for e-bike and user updates. */
public interface EventPublisher {
  String RIDE_UPDATE_ADDRESS_EBIKE = "ride.updates.ebike";
  String RIDE_UPDATE_ADDRESS_USER = "ride.updates.user";

  /**
   * Publishes an update for an e-bike.
   *
   * @param id the ID of the e-bike.
   * @param x the x-coordinate of the e-bike's location.
   * @param y the y-coordinate of the e-bike's location.
   * @param state the state of the e-bike.
   * @param batteryLevel the battery level of the e-bike.
   */
  void publishEBikeUpdate(String id, double x, double y, String state, int batteryLevel);

  /**
   * Publishes an update for a user.
   *
   * @param username the username of the user.
   * @param credit the credit of the user.
   */
  void publishUserUpdate(String username, int credit);
}
