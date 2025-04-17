package infrastructure.persistence;

import application.ports.UserRepository;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.mongo.MongoClient;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class MongoUserRepository implements UserRepository {
    private final MongoClient mongoClient;
    private static final String COLLECTION = "users";

    public MongoUserRepository(MongoClient mongoClient) {
        this.mongoClient = mongoClient;
    }
    @Override
    public CompletableFuture<Void> save(JsonObject user) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (user == null || !user.containsKey("username")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid user data"));
            return future;
        }

        JsonObject document = new JsonObject()
                .put("username", user.getString("username"))
                .put("type", user.getString("type"))
                .put("credit", user.getInteger("credit"));

        mongoClient.insert(COLLECTION, document)
                .onSuccess(result -> future.complete(null))
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to save user: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<Void> update(JsonObject user) {
        CompletableFuture<Void> future = new CompletableFuture<>();

        if (user == null || !user.containsKey("username")) {
            future.completeExceptionally(new IllegalArgumentException("Invalid user data"));
            return future;
        }

        JsonObject query = new JsonObject().put("username", user.getString("username"));
        JsonObject update = new JsonObject().put("$set", user);
        update.remove("username");

        mongoClient.findOneAndUpdate(COLLECTION, query, update)
                .onSuccess(result -> {
                    if (result != null) {
                        future.complete(null);
                    } else {
                        future.completeExceptionally(new RuntimeException("User not found"));
                    }
                }).onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to update user: " + error.getMessage())));

        return future;
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> findByUsername(String username) {
        CompletableFuture<Optional<JsonObject>> future = new CompletableFuture<>();

        if (username == null) {
            future.completeExceptionally(new IllegalArgumentException("Invalid username"));
            return future;
        }

        JsonObject query = new JsonObject().put("username", username);

        mongoClient.findOne(COLLECTION, query, null)
                .onSuccess(result -> {
                    if (result != null) {
                        JsonObject user = new JsonObject()
                                .put("username", result.getString("username"))
                                .put("type", result.getString("type"))
                                .put("credit", result.getInteger("credit"));
                        future.complete(Optional.of(user));
                    } else {
                        future.complete(Optional.empty());
                    }
                })
                .onFailure(error -> future.completeExceptionally(
                        new RuntimeException("Failed to find user: " + error.getMessage())));
        return future;
    }

    @Override
    public CompletableFuture<JsonArray> findAll() {
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonObject query = new JsonObject();

        mongoClient.find(COLLECTION, query)
                .onSuccess(results ->{
                    JsonArray users = new JsonArray();
                    results.forEach(result -> {
                        JsonObject user = new JsonObject()
                                .put("username", result.getString("username"))
                                .put("type", result.getString("type"))
                                .put("credit", result.getInteger("credit"));
                        users.add(user);
                            });
                    future.complete(users);
                })
                .onFailure(future::completeExceptionally);
        return future;
    }
}
