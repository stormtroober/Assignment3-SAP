package infrastructure.persistence;

import application.StationServiceImpl;
import application.ports.StationRepository;
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
            .put("slots", station.getJsonArray("slots", new JsonArray()))
            .put("maxSlots", station.getInteger("maxSlots", StationServiceImpl.MAX_SLOTS));

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
                        .put("slots", result.getJsonArray("slots", new JsonArray()))
                        .put(
                            "maxSlots",
                            result.getInteger("maxSlots", StationServiceImpl.MAX_SLOTS));
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
                            .put("slots", result.getJsonArray("slots", new JsonArray()))
                            .put(
                                "maxSlots",
                                result.getInteger("maxSlots", StationServiceImpl.MAX_SLOTS));
                    stations.add(station);
                  });
              future.complete(stations);
            })
        .onFailure(future::completeExceptionally);

    return future;
  }
}
