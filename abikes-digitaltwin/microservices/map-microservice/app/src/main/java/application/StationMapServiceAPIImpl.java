package application;

import application.ports.EventPublisher;
import application.ports.StationMapServiceAPI;
import domain.model.Station;
import domain.model.repository.StationRepository;
import domain.model.repository.StationRepositoryImpl;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class StationMapServiceAPIImpl implements StationMapServiceAPI {

    private final StationRepository stationRepository;
    private final EventPublisher eventPublisher;

    public StationMapServiceAPIImpl(EventPublisher eventPublisher) {
        this.stationRepository = new StationRepositoryImpl();
        this.eventPublisher = eventPublisher;
    }

    @Override
    public CompletableFuture<Void> updateStation(Station station) {
        return stationRepository.saveStation(station)
            .thenAccept(v -> stationRepository.getAllStations()
                .thenAccept(eventPublisher::publishStationsUpdate));
    }

    @Override
    public CompletableFuture<Void> updateStations(List<Station> stations) {
        return CompletableFuture.allOf(
            stations.stream()
                .map(stationRepository::saveStation)
                .toArray(CompletableFuture[]::new)
        ).thenAccept(v -> stationRepository.getAllStations()
            .thenAccept(eventPublisher::publishStationsUpdate));
    }

    @Override
    public void getAllStations() {
        stationRepository.getAllStations().thenAccept(eventPublisher::publishStationsUpdate);
    }
}