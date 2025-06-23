package application.ports;

import domain.model.Station;

import java.util.concurrent.CompletableFuture;

public interface StationMapServiceAPI {

  CompletableFuture<Void> updateStation(Station station);

  void getAllStations();
}
