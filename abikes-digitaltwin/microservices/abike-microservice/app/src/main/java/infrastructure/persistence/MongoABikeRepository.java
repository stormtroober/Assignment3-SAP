package infrastructure.persistence;

import application.ports.ABikeRepository;
import domain.model.ABike;
import domain.model.ABikeMapper;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoABikeRepository implements ABikeRepository {
  private final MongoClient mongoClient;
  private static final String COLLECTION = "ebikes";

  public MongoABikeRepository(MongoClient mongoClient) {
    this.mongoClient = mongoClient;
  }

    public CompletableFuture<Void> save(ABike aBike) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject abike = ABikeMapper.toJson(aBike);
        if (abike == null || !abike.containsKey("id")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject document = abike.copy();
        document.put("_id", abike.getString("id"));
        document.remove("id");

        mongoClient
                .insert(COLLECTION, document)
                .onSuccess(result -> future.complete(null))
                .onFailure(
                        error -> future.completeExceptionally(
                                new RuntimeException("Failed to save ebike: " + error.getMessage()))
                );

        return future;
    }

  @Override
  public CompletableFuture<Void> update(ABike aBike) {
    CompletableFuture<Void> future = new CompletableFuture<>();
    JsonObject abike = ABikeMapper.toJson(aBike);
    if (abike == null || !abike.containsKey("id")) {
      future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
      return future;
    }

    JsonObject query = new JsonObject().put("_id", abike.getString("id"));

    JsonObject updateDoc = abike.copy();
    updateDoc.remove("id");

    JsonObject update = new JsonObject().put("$set", updateDoc);

    mongoClient
        .findOneAndUpdate(COLLECTION, query, update)
        .onSuccess(
            result -> {
              if (result != null) {
                future.complete(null);
              } else {
                future.completeExceptionally(new RuntimeException("EBike not found"));
              }
            })
        .onFailure(
            error ->
                future.completeExceptionally(
                    new RuntimeException("Failed to update ebike: " + error.getMessage())));

    return future;
  }

    @Override
    public CompletableFuture<Optional<ABike>> findById(String id) {
        CompletableFuture<Optional<ABike>> future = new CompletableFuture<>();

        if (id == null || id.trim().isEmpty()) {
            future.completeExceptionally(new IllegalArgumentException("Invalid id"));
            return future;
        }

        JsonObject query = new JsonObject().put("_id", id);

        mongoClient
                .findOne(COLLECTION, query, null)
                .onSuccess(result -> {
                    if (result != null) {
                        JsonObject abikeJson = new JsonObject()
                                .put("id", result.getString("_id"))
                                .put("state", result.getString("state"))
                                .put("batteryLevel", result.getInteger("batteryLevel"))
                                .put("location", result.getJsonObject("location"))
                                .put("type", result.getString("type"));
                        ABike abike = ABikeMapper.fromJson(abikeJson);
                        future.complete(Optional.of(abike));
                    } else {
                        future.complete(Optional.empty());
                    }
                })
                .onFailure(error ->
                        future.completeExceptionally(
                                new RuntimeException("Failed to find ebike: " + error.getMessage()))
                );

        return future;
    }

  @Override
  public CompletableFuture<List<ABike>> findAll() {
      CompletableFuture<List<ABike>> future = new CompletableFuture<>();
      JsonObject query = new JsonObject();

      mongoClient
          .find(COLLECTION, query)
          .onSuccess(results -> {
              List<ABike> abikes = results.stream()
                  .map(result -> {
                      JsonObject abikeJson = new JsonObject()
                          .put("id", result.getString("_id"))
                          .put("state", result.getString("state"))
                          .put("batteryLevel", result.getInteger("batteryLevel"))
                          .put("location", result.getJsonObject("location"))
                          .put("type", result.getString("type"));
                      return ABikeMapper.fromJson(abikeJson);
                  })
                  .toList();
              future.complete(abikes);
          })
          .onFailure(future::completeExceptionally);

      return future;
  }
}
