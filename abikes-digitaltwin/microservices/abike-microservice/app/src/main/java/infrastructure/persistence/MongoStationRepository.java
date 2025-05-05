package infrastructure.persistence;

import application.StationServiceImpl;
import application.ports.StationRepository;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static domain.model.StationFactory.MAX_SLOTS;

public class MongoStationRepository implements StationRepository {
private final MongoClient mongoClient;
private static final String COLLECTION = "stations";

public MongoStationRepository(MongoClient mongoClient) {
this.mongoClient = mongoClient;
}

@Override
public CompletableFuture<Void> save(JsonObject station) {
CompletableFuture<Void> future = new CompletableFuture<>();

if (station == null || !station.containsKey("id")) {
  future.completeExceptionally(new IllegalArgumentException("Invalid station data"));
  return future;
}

JsonObject document =
    new JsonObject()
        .put("_id", station.getString("id"))
        .put("location", station.getJsonObject("location"))
        .put("slots", convertSlotsToJsonArray(station.getJsonArray("slots", new JsonArray())))
        .put("maxSlots", station.getInteger("maxSlots", MAX_SLOTS));

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
public CompletableFuture<Void> update(JsonObject station) {
CompletableFuture<Void> future = new CompletableFuture<>();

if (station == null || !station.containsKey("id")) {
  future.completeExceptionally(new IllegalArgumentException("Invalid station data"));
  return future;
}

JsonObject query = new JsonObject().put("_id", station.getString("id"));
JsonObject updateDoc = station.copy();
updateDoc.remove("id");
updateDoc.put("slots", convertSlotsToJsonArray(station.getJsonArray("slots", new JsonArray())));
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
public CompletableFuture<Optional<JsonObject>> findById(String id) {
CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();

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
            JsonObject station =
                new JsonObject()
                    .put("id", result.getString("_id"))
                    .put("location", result.getJsonObject("location"))
                    .put("slots", convertSlotsToJsonArray(result.getJsonArray("slots", new JsonArray())))
                    .put(
                        "maxSlots",
                        result.getInteger("maxSlots", MAX_SLOTS));
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
public CompletableFuture<JsonArray> findAll() {
CompletableFuture<JsonArray> future = new CompletableFuture<>();
JsonObject query = new JsonObject();

mongoClient
    .find(COLLECTION, query)
    .onSuccess(
        results -> {
          JsonArray stations = new JsonArray();
          results.forEach(
              result -> {
                JsonObject station =
                    new JsonObject()
                        .put("id", result.getString("_id"))
                        .put("location", result.getJsonObject("location"))
                        .put("slots", convertSlotsToJsonArray(result.getJsonArray("slots", new JsonArray())))
                        .put(
                            "maxSlots",
                            result.getInteger("maxSlots", MAX_SLOTS));
                stations.add(station);
              });
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
    result.add(new JsonObject().put("id", slotId).put("abikeId", (String) null));
  }
}
return result;
}
}