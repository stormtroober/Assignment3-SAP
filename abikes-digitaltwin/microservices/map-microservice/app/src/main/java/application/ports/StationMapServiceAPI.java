package application.ports;

import domain.model.Station;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface StationMapServiceAPI {

  CompletableFuture<Void> updateStation(Station station);

  CompletableFuture<Void> updateStations(List<Station> stations);

  CompletableFuture<Void> deassignBikeFromStation(String bikeId);

  void getAllStations();
}
