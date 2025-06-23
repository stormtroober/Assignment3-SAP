package application;

import application.ports.BikeCommunicationPort;
import application.ports.EBikeRepository;
import application.ports.EBikeServiceAPI;
import domain.model.*;
import infrastructure.adapters.outbound.BikeUpdateAdapter;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class EBikeServiceImpl implements EBikeServiceAPI {

  private final EBikeRepository repository;
  private final BikeCommunicationPort mapCommunicationAdapter;

  public EBikeServiceImpl(EBikeRepository repository, BikeUpdateAdapter bikeUpdateAdapter) {
    this.repository = repository;
    this.mapCommunicationAdapter = bikeUpdateAdapter;
    repository.findAll().thenAccept(bikeUpdateAdapter::sendAllUpdates);
  }

  @Override
  public CompletableFuture<EBike> createEBike(String id, float x, float y) {
    EBike ebike =
        EBikeFactory.getInstance()
            .create(id, new P2d(x, y), EBikeState.AVAILABLE, 100, BikeType.NORMAL);

    mapCommunicationAdapter.sendUpdate(ebike);
    return repository.save(ebike).thenApply(v -> ebike);
  }

  @Override
  public CompletableFuture<Optional<EBike>> getEBike(String id) {
    return repository.findById(id);
  }

  @Override
  public CompletableFuture<EBike> rechargeEBike(String id) {
    return repository
        .findById(id)
        .thenCompose(
            optionalEbike -> {
              if (optionalEbike.isPresent()) {
                EBike ebike = optionalEbike.get();
                EBike newEBike = new EBike(
                    ebike.getId(),
                    ebike.getLocation(),
                    EBikeState.AVAILABLE,
                    EBike.MAX_BATTERY_LEVEL,
                    ebike.getType());
                mapCommunicationAdapter.sendUpdate(ebike);
                return repository.update(newEBike).thenApply(v -> newEBike);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<EBike> updateEBike(EBike ebike) {

    int newBattery = ebike.getBatteryLevel();
    EBikeState newState = ebike.getState();

    if(newBattery < EBike.MAX_BATTERY_LEVEL) {
      if (newBattery == 0) {
        newState = EBikeState.MAINTENANCE;
      }
    }
    var updatedEBike = new EBike(ebike.getId(), ebike.getLocation(), newState, newBattery, ebike.getType());

    return repository
        .update(updatedEBike)
        .thenCompose(
            v ->
                repository
                    .findById(updatedEBike.getId())
                    .thenApply(
                        foundUpdatedEbike -> {
                          if(foundUpdatedEbike.isEmpty()) {
                            throw new RuntimeException("EBike not found after update");
                          }
                          else {
                            var finalEBike = foundUpdatedEbike.get();
                            mapCommunicationAdapter.sendUpdate(finalEBike);
                            return finalEBike;
                          }
                        }));
  }

  @Override
  public CompletableFuture<List<EBike>> getAllEBikes() {
    return repository.findAll();
  }
}
