package application.ports;

import domain.model.ABike;
import domain.model.BikeType;
import domain.model.EBike;
import domain.model.Station;
import java.util.List;

/**
 * Port interface for publishing various domain events related to bikes, stations, and user rides.
 */
public interface EventPublisher {

  /**
   * Publishes an update event for a list of e-bikes.
   *
   * @param bikes the list of {@link EBike} instances to update
   */
  void publishBikesUpdate(List<EBike> bikes);

  /**
   * Publishes an update event for a list of a-bikes.
   *
   * @param bikes the list of {@link ABike} instances to update
   */
  void publishABikesUpdate(List<ABike> bikes);

  /**
   * Publishes an update event for a user's a-bikes.
   *
   * @param bikes the list of {@link ABike} instances for the user
   * @param username the username of the user
   */
  void publishUserABikesUpdate(List<ABike> bikes, String username);

  /**
   * Publishes an update event for a user's e-bikes.
   *
   * @param bikes the list of {@link EBike} instances for the user
   * @param username the username of the user
   */
  void publishUserEBikesUpdate(List<EBike> bikes, String username);

  /**
   * Publishes an update event for the available e-bikes of a user.
   *
   * @param bikes the list of available {@link EBike} instances for the user
   */
  void publishUserAvailableEBikesUpdate(List<EBike> bikes);

  /**
   * Publishes an update event for the available a-bikes of a user.
   *
   * @param bikes the list of available {@link ABike} instances for the user
   */
  void publishUserAvailableABikesUpdate(List<ABike> bikes);

  /**
   * Publishes an event to notify a user that their ride has been forcibly stopped.
   *
   * @param username the username of the user whose ride is to be stopped
   * @param bikeType the type of bike involved in the ride
   */
  void publishStopRide(String username, BikeType bikeType);

  /**
   * Publishes an update event for a list of public a-bikes.
   *
   * @param bikes the list of public {@link ABike} instances to update
   */
  void publishPublicABikesUpdate(List<ABike> bikes);

  /**
   * Publishes an update event for a list of stations.
   *
   * @param stations the list of {@link Station} instances to update
   */
  void publishStationsUpdate(List<Station> stations);
}
