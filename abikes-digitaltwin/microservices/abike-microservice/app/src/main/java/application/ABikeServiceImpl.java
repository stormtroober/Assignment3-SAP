package application;

import application.ports.ABikeRepository;
import application.ports.ABikeServiceAPI;
import application.ports.BikeCommunicationPort;
import application.ports.StationServiceAPI;
import domain.model.*;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

public class ABikeServiceImpl implements ABikeServiceAPI {

  private final ABikeRepository repository;
  private final BikeCommunicationPort bikeCommunicationAdapter;
  private final StationServiceAPI stationService;
  private final Random random = new Random();
  public static final Integer MAX_BATTERY = 300;

  public ABikeServiceImpl(
      ABikeRepository repository,
      BikeCommunicationPort bikeCommunicationAdapter,
      StationServiceAPI stationService) {
    this.repository = repository;
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.stationService = stationService;
    repository.findAll().thenAccept(bikeCommunicationAdapter::sendAllUpdates);
  }

  // Get all stations and pick a random one for the bike's location
  @Override
  public CompletableFuture<ABike> createABike(String bikeId) {
    return stationService
        .findStationWithFreeSlot()
        .thenCompose(
            optStation ->
                optStation
                    .map(
                        station ->
                            stationService
                                .assignBikeToStation(station.getId(), bikeId)
                                .thenApply(st -> st != null ? station.getPosition() : null))
                    .orElseGet(
                        () ->
                            CompletableFuture.completedFuture(
                                new P2d(100 * random.nextDouble(), 100 * random.nextDouble()))))
        .thenCompose(
            position -> {
              P2d bikePosition =
                  position != null
                      ? position
                      : new P2d(100 * random.nextDouble(), 100 * random.nextDouble());
              ABike abike =
                  ABikeFactory.getInstance()
                      .create(
                          bikeId,
                          bikePosition,
                          ABikeState.AVAILABLE,
                          MAX_BATTERY,
                          BikeType.AUTONOMOUS);
              bikeCommunicationAdapter.sendUpdate(abike);
              return repository.save(abike).thenApply(v -> abike);
            });
  }

  @Override
  public CompletableFuture<ABike> rechargeABike(String id) {
    return repository
        .findById(id)
        .thenCompose(
            optionalABike -> {
              if (optionalABike.isPresent()) {
                ABike abike = optionalABike.get();
                ABike recharged =
                    new ABike(
                        abike.getId(),
                        abike.getLocation(),
                        ABikeState.AVAILABLE,
                        MAX_BATTERY,
                        abike.getType());
                bikeCommunicationAdapter.sendUpdate(recharged);
                return repository.update(recharged).thenApply(v -> recharged);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<ABike> updateABike(ABike abike) {
    ABike updatedABike;
    if (abike.getBatteryLevel() == 0 && abike.getABikeState() != ABikeState.MAINTENANCE) {
      updatedABike =
          new ABike(
              abike.getId(),
              abike.getLocation(),
              ABikeState.MAINTENANCE,
              abike.getBatteryLevel(),
              abike.getType());
    } else {
      updatedABike = abike;
    }
    return repository
        .update(updatedABike)
        .thenCompose(
            v ->
                repository
                    .findById(updatedABike.getId())
                    .thenApply(
                        opt -> {
                          bikeCommunicationAdapter.sendUpdate(updatedABike);
                          return updatedABike;
                        }));
  }
}
