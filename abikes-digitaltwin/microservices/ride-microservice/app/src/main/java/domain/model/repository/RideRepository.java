package domain.model.repository;

import domain.model.P2d;
import domain.model.Ride;
import domain.model.simulation.RideSimulation;
import java.util.Optional;

public interface RideRepository {

  void addNormalRide(Ride ride, SimulationType type, Optional<P2d> destination);

  void removeRide(Ride ride);

  /**
   * Adds a ride with a simulation to the repository. Alternative to the addNormalRide method
   *
   * @param ride
   * @param simulation
   */
  void addRideWithSimulation(Ride ride, RideSimulation simulation);

  RideSimulation getRideSimulation(String rideId);

  RideSimulation getRideSimulationByUserId(String userId);
}
