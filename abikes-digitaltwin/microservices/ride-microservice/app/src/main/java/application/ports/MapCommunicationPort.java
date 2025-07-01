package application.ports;

import domain.model.bike.BikeType;

/**
 * Port interface for communicating with the map microservice adapter. Defines operations for
 * notifying ride events and initializing the communication channel.
 */
public interface MapCommunicationPort {

  /**
   * Notifies the start of a ride for a specific bike and user.
   *
   * @param bikeId the ID of the bike
   * @param type the {@link BikeType} of the bike
   * @param userId the ID of the user
   */
  void notifyStartRide(String bikeId, BikeType type, String userId);

  /**
   * Notifies the end of a ride for a specific bike and user.
   *
   * @param bikeId the ID of the bike
   * @param type the {@link BikeType} of the bike
   * @param userId the ID of the user
   */
  void notifyEndRide(String bikeId, BikeType type, String userId);

  /**
   * Notifies the start of a public ride for a specific bike.
   *
   * @param bikeId the ID of the bike
   * @param type the {@link BikeType} of the bike
   */
  void notifyStartPublicRide(String bikeId, BikeType type);

  /**
   * Notifies the end of a public ride for a specific bike.
   *
   * @param bikeId the ID of the bike
   * @param type the {@link BikeType} of the bike
   */
  void notifyEndPublicRide(String bikeId, BikeType type);

  /** Initializes the communication port. */
  void init();
}
