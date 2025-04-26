package application;

import application.ports.ABikeServiceAPI;
import application.ports.EBikeRepository;
import application.ports.MapCommunicationPort;
import application.ports.StationServiceAPI;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABikeServiceImpl implements ABikeServiceAPI {

  private final EBikeRepository repository;
  private final MapCommunicationPort mapCommunicationAdapter;
  private final StationServiceAPI stationService;
  private final Random random = new Random();
  private static final Logger logger = LoggerFactory.getLogger(ABikeServiceImpl.class);

  public ABikeServiceImpl(
      EBikeRepository repository,
      MapCommunicationPort mapCommunicationAdapter,
      StationServiceAPI stationService) {
    this.repository = repository;
    this.mapCommunicationAdapter = mapCommunicationAdapter;
    this.stationService = stationService;
    repository.findAll().thenAccept(mapCommunicationAdapter::sendAllUpdates);
  }

  @Override
  public CompletableFuture<JsonObject> createABike(String id) {
    // Get all stations and pick a random one for the bike's location
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
              JsonObject location = station.getJsonObject("location");
              JsonObject abike =
                  new JsonObject()
                      .put("id", id)
                      .put("state", "AVAILABLE")
                      .put("batteryLevel", 100)
                      .put("location", location);
              mapCommunicationAdapter.sendUpdate(abike);
              return repository
                  .save(abike)
                  .thenApply(
                      v -> {
                        logger.info("Saved bike successfully: {}", abike.encode());
                        return abike;
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
