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

  void notifyStartPublicRide(String bikeId, BikeType type);

  void notifyEndPublicRide(String bikeId, BikeType type);

  /** Initializes the communication port. */
  void init();
}
