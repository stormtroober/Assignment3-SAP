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
  private final CommunicationPort mapCommunicationAdapter;
  private final StationServiceAPI stationService;
  private final Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(ABikeServiceImpl.class);

  public ABikeServiceImpl(
      ABikeRepository repository,
      CommunicationPort mapCommunicationAdapter,
      StationServiceAPI stationService) {
    this.repository = repository;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.stationService = stationService;
    repository.findAll().thenAccept(mapCommunicationAdapter::sendAllUpdates);
  }

  // Get all stations and pick a random one for the bike's location
  @Override
  public CompletableFuture<JsonObject> createABike(String id) {
    return stationService
        .getAllStations()
        .thenCompose(
            stations -> {
              if (stations.isEmpty()) {
                CompletableFuture<JsonObject> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("No stations available"));
                return failed;
              }
              int idx = random.nextInt(stations.size());
              JsonObject station = stations.getJsonObject(idx);
              JsonObject locationJson = station.getJsonObject("location");
              // Use the singleton factory to create an ABike domain object
              ABike abike =
                  ABikeFactory.getInstance()
                      .create(
                          id,
                          new P2d(locationJson.getDouble("x"), locationJson.getDouble("y")),
                          ABikeState.AVAILABLE,
                          100,
                              BikeType.AUTONOMOUS);
              JsonObject abikeJson =
                  new JsonObject()
                      .put("id", abike.getId())
                      .put("state", abike.getABikeState().name())
                      .put("batteryLevel", abike.getBatteryLevel())
                      .put("location", locationJson)
                        .put("type", abike.getType().name());
              mapCommunicationAdapter.sendUpdate(abikeJson);
              return repository
                  .save(abikeJson)
                  .thenApply(
                      v -> {
                        logger.info("Saved bike successfully: {}", abikeJson.encode());
                        return abikeJson;
                      });
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
                mapCommunicationAdapter.sendUpdate(abike);
                return repository.update(abike).thenApply(v -> abike);
              }
              return CompletableFuture.completedFuture(null);
            });
  }

  @Override
  public CompletableFuture<JsonObject> requestABikeAtLocation(float x, float y, String userId) {
    return repository
        .findAll()
        .thenCompose(
            abikesArray -> {
              // Find available bikes with sufficient battery
              JsonObject closestBike = null;
              double minDistance = Double.MAX_VALUE;
              for (int i = 0; i < abikesArray.size(); i++) {
                JsonObject bike = abikesArray.getJsonObject(i);
                if (!"AVAILABLE".equals(bike.getString("state"))) continue;
                int battery = bike.getInteger("batteryLevel", 0);
                if (battery < 20) continue; // threshold, adjust as needed
                JsonObject loc = bike.getJsonObject("location");
                double dist = Math.hypot(loc.getDouble("x") - x, loc.getDouble("y") - y);
                if (dist < minDistance) {
                  minDistance = dist;
                  closestBike = bike;
                }
              }
              if (closestBike == null) {
                CompletableFuture<JsonObject> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("No available bikes nearby"));
                return failed;
              }
              // Update bike state and destination
              closestBike
                  .put("state", "MOVING_TO_USER")
                  .put("reservedBy", userId)
                  .put("destination", new JsonObject().put("x", x).put("y", y));
              mapCommunicationAdapter.sendUpdate(closestBike);
              JsonObject finalClosestBike = closestBike;
              return repository.update(closestBike).thenApply(v -> finalClosestBike);
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
                          mapCommunicationAdapter.sendUpdate(updatedABike.orElse(abike));
                          return abike;
                        }));
  }

  @Override
  public CompletableFuture<JsonArray> getAllABikes() {
    return repository.findAll();
  }
}
