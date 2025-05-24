package application.ports;

import domain.model.ABike;
import domain.model.BikeType;
import domain.model.EBike;
import domain.model.Station;
import java.util.List;

/** Port representing an event publisher for e-bike updates and user ride events. */
public interface EventPublisher {

  /**
   * Publishes an update for a list of e-bikes.
   *
   * @param bikes the list of e-bikes to update.
   */
  void publishBikesUpdate(List<EBike> bikes);

  void publishABikesUpdate(List<ABike> bikes);

  void publishUserABikesUpdate(List<ABike> bikes, String username);

  /**
   * Publishes an update for a user's e-bikes.
   *
   * @param bikes the list of e-bikes for the user.
   * @param username the username of the user.
   */
  void publishUserEBikesUpdate(List<EBike> bikes, String username);

  /**
   * Publishes an update for the available e-bikes of a user.
   *
   * @param bikes the list of available e-bikes for the user.
   */
  void publishUserAvailableEBikesUpdate(List<EBike> bikes);

    /**
     * Publishes an update for the available a-bikes of a user.
     *
     * @param bikes the list of available a-bikes for the user.
     */
  void publishUserAvailableABikesUpdate(List<ABike> bikes);

  /**
   * Publishes an event to notify a user that his ride has been forced to stop.
   *
   * @param username the username of the user whose ride is to be stopped.
   * @param bikeType
   */
  void publishStopRide(String username, BikeType bikeType);

  void publishPublicABikesUpdate(List<ABike> bikes);

  /**
   * Publishes an update for a list of stations.
   *
   * @param stations the list of stations to update.
   */
  void publishStationsUpdate(List<Station> stations);
}
