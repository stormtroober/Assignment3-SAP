package domain.model.repository;

import domain.model.P2d;
import domain.model.Ride;
import domain.model.simulation.RideSimulation;
import java.util.Optional;

public interface RideRepository {

  void addRide(Ride ride, SimulationType type, Optional<P2d> destination);

  void removeRide(Ride ride);

  /**
   * Sets a specific simulation for a ride.
   * This is useful when using custom or compound simulations.
   *
   * @param rideId The ride ID
   * @param simulation The simulation to set
   */
  void setRideSimulation(String rideId, RideSimulation simulation);

  RideSimulation getRideSimulation(String rideId);

  RideSimulation getRideSimulationByUserId(String userId);
}
