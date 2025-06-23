package infrastructure.persistence;

import application.ports.EBikeRepository;
import domain.model.EBike;
import domain.model.EBikeMapper;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoEBikeRepository implements EBikeRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "ebikes";

    public MongoEBikeRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }

    @Override
    public CompletableFuture<Void> save(EBike eBike) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject ebike = EBikeMapper.toJson(eBike);
        if (ebike == null || !ebike.containsKey("id")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject document = ebike.copy();
        document.put("_id", ebike.getString("id"));
        document.remove("id");

        mongoClient
                .insert(COLLECTION, document)
                .onSuccess(result -> future.complete(null))
                .onFailure(
                        error ->
                                future.completeExceptionally(
                                        new RuntimeException("Failed to save ebike: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<Void> update(EBike eBike) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        JsonObject ebike = EBikeMapper.toJson(eBike);
        if (ebike == null || !ebike.containsKey("id")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid ebike data"));
            return future;
        }

        JsonObject query = new JsonObject().put("_id", ebike.getString("id"));

        JsonObject updateDoc = ebike.copy();
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
    public CompletableFuture<Optional<EBike>> findById(String id) {
        CompletableFuture<Optional<EBike>> future = new CompletableFuture<>();

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
                                JsonObject ebikeJson =
                                        new JsonObject()
                                                .put("id", result.getString("_id"))
                                                .put("state", result.getString("state"))
                                                .put("batteryLevel", result.getInteger("batteryLevel"))
                                                .put("location", result.getJsonObject("location"))
                                                .put("type", result.getString("type"));
                                EBike ebike = EBikeMapper.fromJson(ebikeJson);
                                future.complete(Optional.of(ebike));
                            } else {
                                future.complete(Optional.empty());
                            }
                        })
                .onFailure(
                        error ->
                                future.completeExceptionally(
                                        new RuntimeException("Failed to find ebike: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<List<EBike>> findAll() {
        CompletableFuture<List<EBike>> future = new CompletableFuture<>();
        JsonObject query = new JsonObject();

        mongoClient
                .find(COLLECTION, query)
                .onSuccess(
                        results -> {
                            List<EBike> ebikes = results.stream()
                                    .map(result -> {
                                        JsonObject ebikeJson = new JsonObject()
                                                .put("id", result.getString("_id"))
                                                .put("state", result.getString("state"))
                                                .put("batteryLevel", result.getInteger("batteryLevel"))
                                                .put("location", result.getJsonObject("location"))
                                                .put("type", result.getString("type"));
                                        return EBikeMapper.fromJson(ebikeJson);
                                    })
                                    .toList();
                            future.complete(ebikes);
                        })
                .onFailure(future::completeExceptionally);

        return future;
    }
}