package infrastructure.persistence;

import application.ports.EBikeRepository;
import infrastructure.adapters.map.MapCommunicationAdapter;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoEBikeRepository implements EBikeRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "ebikes";

    public MongoEBikeRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public CompletableFuture<Void> save(JsonObject ebike) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (ebike == null || !ebike.containsKey("id")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject document = new JsonObject()
                .put("_id", ebike.getString("id"))
                .put("state", ebike.getString("state"))
                .put("batteryLevel", ebike.getInteger("batteryLevel"))
                .put("location", ebike.getJsonObject("location"));

        mongoClient.insert(COLLECTION, document)
                .onSuccess(result -> future.complete(null))
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to save ebike: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<Void> update(JsonObject ebike) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (ebike == null || !ebike.containsKey("id")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject query = new JsonObject().put("_id", ebike.getString("id"));

        JsonObject updateDoc = ebike.copy();
        updateDoc.remove("id");

        JsonObject update = new JsonObject().put("$set", updateDoc);

        mongoClient.findOneAndUpdate(COLLECTION, query, update)
                .onSuccess(result -> {
                    if (result != null) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(new RuntimeException("EBike not found"));
                    }
                })
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to update ebike: " + error.getMessage())));

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

        mongoClient.findOne(COLLECTION, query, null)
                .onSuccess(result -> {
                    if (result != null) {
                        JsonObject ebike = new JsonObject()
                                .put("id", result.getString("_id"))
                                .put("state", result.getString("state"))
                                .put("batteryLevel", result.getInteger("batteryLevel"))
                                .put("location", result.getJsonObject("location"));
                        future.complete(Optional.of(ebike));
                    } else {
                        future.complete(Optional.empty());
                    }
                })
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to find ebike: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<JsonArray> findAll() {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonObject query = new JsonObject();

        mongoClient.find(COLLECTION, query)
                .onSuccess(results -> {
                    JsonArray ebikes = new JsonArray();
                    results.forEach(result -> {
                        JsonObject ebike = new JsonObject()
                                .put("id", result.getString("_id"))
                                .put("state", result.getString("state"))
                                .put("batteryLevel", result.getInteger("batteryLevel"))
                                .put("location", result.getJsonObject("location"));
                        ebikes.add(ebike);
                    });
                    future.complete(ebikes);
                })
                .onFailure(future::completeExceptionally);

        return future;
    }
}