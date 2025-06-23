package application;

import application.ports.BikeCommunicationPort;
import application.ports.EBikeRepository;
import application.ports.EBikeServiceAPI;
import domain.model.*;
import infrastructure.adapters.outbound.BikeUpdateAdapter;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
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
  public CompletableFuture<JsonObject> createEBike(String id, float x, float y) {
    EBike ebike =
        EBikeFactory.getInstance()
            .create(id, new P2d(x, y), EBikeState.AVAILABLE, 100, BikeType.NORMAL);
    JsonObject ebikeJson =
        new JsonObject()
            .put("id", ebike.getId())
            .put("state", ebike.getState().name())
            .put("batteryLevel", ebike.getBatteryLevel())
            .put("location", new JsonObject().put("x", x).put("y", y))
            .put("type", ebike.getType().name());
    mapCommunicationAdapter.sendUpdate(ebikeJson);
    return repository.save(ebikeJson).thenApply(v -> ebikeJson);
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> getEBike(String id) {
    return repository.findById(id);
  }

  @Override
  public CompletableFuture<JsonObject> rechargeEBike(String id) {
    return repository
        .findById(id)
        .thenCompose(
            optionalEbike -> {
              if (optionalEbike.isPresent()) {
                JsonObject ebike = optionalEbike.get();
                ebike.put("batteryLevel", 100).put("state", "AVAILABLE");
                mapCommunicationAdapter.sendUpdate(ebike);
                return repository.update(ebike).thenApply(v -> ebike);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<JsonObject> updateEBike(EBike ebike) {

    int newBattery = ebike.getBatteryLevel();
    EBikeState newState = ebike.getState();

    if(newBattery < EBike.MAX_BATTERY_LEVEL) {
      if (newBattery == 0) {
        newState = EBikeState.MAINTENANCE;
      }
    }
    var updatedEBike = new EBike(ebike.getId(), ebike.getLocation(), newState, newBattery, ebike.getType());

    //TODO: this is temporary
    var bikeJson = EBikeMapper.toJson(updatedEBike);

    return repository
        .update(bikeJson)
        .thenCompose(
            v ->
                repository
                    .findById(updatedEBike.getId())
                    .thenApply(
                        foundUpdatedEbike -> {
                          mapCommunicationAdapter.sendUpdate(foundUpdatedEbike.orElse(bikeJson));
                          return foundUpdatedEbike.get();
                        }));
  }

  @Override
  public CompletableFuture<JsonArray> getAllEBikes() {
    return repository.findAll();
  }
}
