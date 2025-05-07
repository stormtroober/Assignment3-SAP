package domain.model.repository;

import domain.model.Ride;
import domain.model.simulation.RideSimulation;

public interface RideRepository {
  void addRide(Ride ride);

  void removeRide(Ride ride);

  Ride getRide(String rideId);

  RideSimulation getRideSimulation(String rideId);

  RideSimulation getRideSimulationByUserId(String userId);
}
