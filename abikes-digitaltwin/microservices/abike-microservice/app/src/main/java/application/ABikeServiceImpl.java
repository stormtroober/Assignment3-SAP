package application;

import application.ports.ABikeRepository;
import application.ports.ABikeServiceAPI;
import application.ports.CommunicationPort;
import application.ports.StationServiceAPI;
import domain.model.*;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABikeServiceImpl implements ABikeServiceAPI {

  private final ABikeRepository repository;
  private final CommunicationPort bikeCommunicationAdapter;
  private final StationServiceAPI stationService;
  private final Random random = new Random();
  public static final Integer MAX_BATTERY = 100;
  private static final Logger logger = LoggerFactory.getLogger(ABikeServiceImpl.class);

  public ABikeServiceImpl(
      ABikeRepository repository,
      CommunicationPort bikeCommunicationAdapter,
      StationServiceAPI stationService) {
    this.repository = repository;
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.stationService = stationService;
    repository.findAll().thenAccept(bikeCommunicationAdapter::sendAllUpdates);
  }

  // Get all stations and pick a random one for the bike's location
  @Override
  public CompletableFuture<JsonObject> createABike(String id) {
      return stationService.findStationWithFreeSlot().thenCompose(optStation -> {
          if (optStation.isPresent()) {
              JsonObject station = optStation.get();
              String stationId = station.getString("id");
              // Assign bike to station using the service method
              return stationService.assignBikeToStation(stationId, id)
                      .thenCompose(updatedStation -> {
                          JsonObject locationJson = updatedStation.getJsonObject("location");
                          ABike abike = ABikeFactory.getInstance().create(
                                  id,
                                  new P2d(locationJson.getDouble("x"), locationJson.getDouble("y")),
                                  ABikeState.AVAILABLE,
                                  MAX_BATTERY,
                                  BikeType.AUTONOMOUS
                          );
                          JsonObject abikeJson = new JsonObject()
                                  .put("id", abike.getId())
                                  .put("state", abike.getABikeState().name())
                                  .put("batteryLevel", abike.getBatteryLevel())
                                  .put("location", locationJson)
                                  .put("type", abike.getType().name());
                          bikeCommunicationAdapter.sendUpdate(abikeJson);
                          return repository.save(abikeJson).thenApply(v -> abikeJson);
                      });
          } else {
              // All slots full: random location
              double x = 100 * random.nextDouble();
              double y = 100 * random.nextDouble();
              JsonObject locationJson = new JsonObject().put("x", x).put("y", y);
              ABike abike = ABikeFactory.getInstance().create(
                      id,
                      new P2d(locationJson.getDouble("x"), locationJson.getDouble("y")),
                      ABikeState.AVAILABLE,
                      MAX_BATTERY,
                      BikeType.AUTONOMOUS
              );
              JsonObject abikeJson = new JsonObject()
                      .put("id", abike.getId())
                      .put("state", abike.getABikeState().name())
                      .put("batteryLevel", abike.getBatteryLevel())
                      .put("location", locationJson)
                      .put("type", abike.getType().name());
              bikeCommunicationAdapter.sendUpdate(abikeJson);
              return repository.save(abikeJson).thenApply(v -> abikeJson);
          }
      });
  }

  @Override
  public CompletableFuture<Optional<JsonObject>> getABike(String id) {
    return repository.findById(id);
  }

  @Override
  public CompletableFuture<JsonObject> rechargeABike(String id) {
    return repository
        .findById(id)
        .thenCompose(
            optionalABike -> {
              if (optionalABike.isPresent()) {
                JsonObject abike = optionalABike.get();
                abike.put("batteryLevel", 100).put("state", "AVAILABLE");
                bikeCommunicationAdapter.sendUpdate(abike);
                return repository.update(abike).thenApply(v -> abike);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<JsonObject> updateABike(JsonObject abike) {
    if (abike.containsKey("batteryLevel")) {
      int newBattery = abike.getInteger("batteryLevel");
      int currentBattery = abike.getInteger("batteryLevel");
      if (newBattery < currentBattery) {
        abike.put("batteryLevel", newBattery);
        if (newBattery == 0) {
          abike.put("state", "MAINTENANCE");
        }
      }
    }
    if (abike.containsKey("state")) {
      abike.put("state", abike.getString("state"));
    }
    if (abike.containsKey("location")) {
      abike.put("location", abike.getJsonObject("location"));
    }
    return repository
        .update(abike)
        .thenCompose(
            v ->
                repository
                    .findById(abike.getString("id"))
                    .thenApply(
                        updatedABike -> {
                          bikeCommunicationAdapter.sendUpdate(updatedABike.orElse(abike));
                          return abike;
                        }));
  }

  @Override
  public CompletableFuture<JsonArray> getAllABikes() {
    return repository.findAll();
  }
}
