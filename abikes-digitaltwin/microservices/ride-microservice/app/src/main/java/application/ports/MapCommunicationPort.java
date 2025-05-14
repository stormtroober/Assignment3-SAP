package application.ports;

import domain.model.bike.BikeType;

/** Port for communicating with the map microservice adapter. */
public interface MapCommunicationPort {

  /**
   * Notifies the start of a ride for a specific e-bike and user.
   *
   * @param bikeId the ID of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyStartRide(String bikeId, BikeType type, String userId);

  /**
   * Notifies the end of a ride for a specific e-bike and user.
   *
   * @param bikeId the ID of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyEndRide(String bikeId, BikeType type, String userId);

  /**
   * Notifies the start of a ride to go to a user.
   *
   * @param bikeId the ID of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyStartRideToUser(String bikeId, BikeType type, String userId);

  /**
   * Notifies the stop of a ride to go to a user.
   *
   * @param bikeId the ID of the e-bike.
   * @param type the type of the e-bike.
   * @param userId the ID of the user.
   */
  void notifyStopRideToUser(String bikeId, BikeType type, String userId);

  /** Initializes the communication port. */
  void init();
}
