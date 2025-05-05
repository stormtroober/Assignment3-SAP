package application.ports;

/** Port for communicating with the map microservice adapter. */
public interface MapCommunicationPort {

  /**
   * Notifies the start of a ride for a specific e-bike and user.
   *
   * @param bikeId the ID of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyStartRide(String bikeId, String userId);

  /**
   * Notifies the end of a ride for a specific e-bike and user.
   *
   * @param bikeId the ID of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyEndRide(String bikeId, String userId);

    /**
     * Notifies the user about a ride call for a specific e-bike and user.
     *
     * @param bikeId the ID of the e-bike.
     * @param userId the ID of the user.
     */
  void notifyUserRideCall(String bikeId, String userId);

  /** Initializes the communication port. */
  void init();
}
