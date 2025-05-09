package domain.model.repository;

import domain.model.P2d;
import domain.model.Ride;
import domain.model.simulation.RideSimulation;

import java.util.Optional;

public interface RideRepository {

  void addRide(Ride ride, SimulationType type, Optional<P2d> destination);

  void removeRide(Ride ride);

  Ride getRide(String rideId);

  RideSimulation getRideSimulation(String rideId);

  RideSimulation getRideSimulationByUserId(String userId);

}
