package infrastructure.persistence;

import static domain.model.StationFactory.MAX_SLOTS;

import application.ports.StationRepository;
import domain.model.Station;
import domain.model.StationMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoStationRepository implements StationRepository {
  private final MongoClient mongoClient;
  private static final String COLLECTION = "stations";

  public MongoStationRepository(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

  @Override
  public CompletableFuture<Void> save(Station station) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (station == null || station.getId() == null) {
      future.completeExceptionally(new IllegalArgumentException("Invalid station data"));
      return future;
    }

    JsonObject document = StationMapper.toJson(station);
    document.put("_id", station.getId());
    document.remove("id"); // Mongo uses _id

    mongoClient
        .insert(COLLECTION, document)
        .onSuccess(result -> future.complete(null))
        .onFailure(
            error ->
                future.completeExceptionally(
                    new RuntimeException("Failed to save station: " + error.getMessage())));

    return future;
  }

  @Override
  public CompletableFuture<Void> update(Station station) {
    CompletableFuture<Void> future = new CompletableFuture<>();

    if (station == null || station.getId() == null) {
      future.completeExceptionally(new IllegalArgumentException("Invalid station data"));
      return future;
    }

    JsonObject updateDoc = StationMapper.toJson(station);
    updateDoc.put("_id", station.getId());
    updateDoc.remove("id"); // Mongo uses _id

    JsonObject query = new JsonObject().put("_id", station.getId());
    JsonObject update = new JsonObject().put("$set", updateDoc);

    mongoClient
        .findOneAndUpdate(COLLECTION, query, update)
        .onSuccess(
            result -> {
              if (result != null) {
                future.complete(null);
              } else {
                future.completeExceptionally(new RuntimeException("Station not found"));
              }
            })
        .onFailure(
            error ->
                future.completeExceptionally(
                    new RuntimeException("Failed to update station: " + error.getMessage())));

    return future;
  }

  @Override
  public CompletableFuture<Optional<Station>> findById(String id) {
    CompletableFuture<Optional<Station>> future = new CompletableFuture<>();

    if (id == null || id.trim().isEmpty()) {
      future.completeExceptionally(new IllegalArgumentException("Invalid id"));
      return future;
    }

    JsonObject query = new JsonObject().put("_id", id);

    mongoClient
        .findOne(COLLECTION, query, null)
        .onSuccess(
            result -> {
              if (result != null) {
                JsonObject stationJson =
                    new JsonObject()
                        .put("id", result.getString("_id"))
                        .put("location", result.getJsonObject("location"))
                        .put(
                            "slots",
                            convertSlotsToJsonArray(result.getJsonArray("slots", new JsonArray())))
                        .put("maxSlots", result.getInteger("maxSlots", MAX_SLOTS));
                Station station = StationMapper.fromJson(stationJson);
                future.complete(Optional.of(station));
              } else {
                future.complete(Optional.empty());
              }
            })
        .onFailure(
            error ->
                future.completeExceptionally(
                    new RuntimeException("Failed to find station: " + error.getMessage())));

    return future;
  }

  @Override
  public CompletableFuture<java.util.List<Station>> findAll() {
    CompletableFuture<java.util.List<Station>> future = new CompletableFuture<>();
    JsonObject query = new JsonObject();

    mongoClient
        .find(COLLECTION, query)
        .onSuccess(
            results -> {
              java.util.List<Station> stations = new java.util.ArrayList<>();
              for (JsonObject result : results) {
                JsonObject stationJson =
                    new JsonObject()
                        .put("id", result.getString("_id"))
                        .put("location", result.getJsonObject("location"))
                        .put(
                            "slots",
                            convertSlotsToJsonArray(result.getJsonArray("slots", new JsonArray())))
                        .put("maxSlots", result.getInteger("maxSlots", MAX_SLOTS));
                stations.add(StationMapper.fromJson(stationJson));
              }
              future.complete(stations);
            })
        .onFailure(future::completeExceptionally);

    return future;
  }

  // Helper to ensure slots are stored as array of objects with id and abikeId
  private JsonArray convertSlotsToJsonArray(JsonArray slots) {
    JsonArray result = new JsonArray();
    for (int i = 0; i < slots.size(); i++) {
      Object slotObj = slots.getValue(i);
      if (slotObj instanceof JsonObject slotJson) {
        // Already in correct format
        result.add(slotJson);
      } else if (slotObj instanceof String slotId) {
        // Convert string to object with null abikeId
        result.add(new JsonObject().put("id", slotId).put("abikeId", null));
      }
    }
    return result;
  }
}
