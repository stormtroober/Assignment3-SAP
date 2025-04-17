package application;

import application.ports.UserServiceAPI;
import application.ports.UserRepository;
import application.ports.UserEventPublisher;

import domain.model.User;
import io.vertx.core.json.JsonObject;
import io.vertx.core.json.JsonArray;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class UserServiceImpl implements UserServiceAPI {

    private final UserRepository repository;
    private final UserEventPublisher UserEventPublisher;

    public UserServiceImpl(UserRepository repository, UserEventPublisher UserEventPublisher) {
        this.repository = repository;
        this.UserEventPublisher = UserEventPublisher;
    }

    @Override
    public CompletableFuture<JsonObject> signIn(String username) {
        return repository.findByUsername(username).thenApply(user -> {
            if (user.isPresent()) {
                return user.get();
            } else {
                System.out.println("User not found");
            }
            return null;
        });
    }

    @Override
    public CompletableFuture<JsonObject> signUp(String username, User.UserType type) {
        return repository.findByUsername(username).thenCompose(optionalUser -> {
            if (optionalUser.isPresent()) {
                CompletableFuture<JsonObject> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("User already exists"));
                return future;
            } else {
                int credit = 100;
                JsonObject user = new JsonObject()
                        .put("username", username)
                        .put("type", type.toString())
                        .put("credit", credit);

                return repository.save(user).thenApply(v -> {
                    UserEventPublisher.publishUserUpdate(username, user);
                    UserEventPublisher.publishAllUsersUpdates(user);
                    return user;
                });
            }
        });
    }

    @Override
    public CompletableFuture<Optional<JsonObject>> getUserByUsername(String username) {
        return repository.findByUsername(username);
    }

    @Override
    public CompletableFuture<JsonObject> updateUser(JsonObject user) {
        System.out.println("Updating user: " + user);
        String username = user.getString("username");
        if (username == null || username.trim().isEmpty()) {
            CompletableFuture<JsonObject> future = new CompletableFuture<>();
            future.completeExceptionally(new IllegalArgumentException("Invalid username"));
            return future;
        }

        return repository.findByUsername(username).thenCompose(optionalUser -> {
            if (optionalUser.isPresent()) {
                JsonObject existingUser = optionalUser.get();
                if (user.containsKey("credit")) {
                    int newCredit = user.getInteger("credit");
                    existingUser.put("credit", newCredit);
                }
                return repository.update(existingUser).thenApply(v -> {
                    UserEventPublisher.publishUserUpdate(username, existingUser);
                    UserEventPublisher.publishAllUsersUpdates(existingUser);
                    return existingUser;
                });
            } else {
                CompletableFuture<JsonObject> future = new CompletableFuture<>();
                future.completeExceptionally(new RuntimeException("User not found"));
                return future;
            }
        });
    }

    @Override
    public CompletableFuture<JsonObject> rechargeCredit(String username, int creditToAdd) {
        return repository.findByUsername(username).thenCompose(optionalUser -> {
            if (optionalUser.isPresent()) {
                JsonObject user = optionalUser.get();
                int currentCredit = user.getInteger("credit");
                user.put("credit", currentCredit + creditToAdd);
                return repository.update(user).thenApply(v -> {
                    UserEventPublisher.publishUserUpdate(username, user);
                    UserEventPublisher.publishAllUsersUpdates(user);
                    return user;
                });
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletableFuture<JsonObject> decreaseCredit(String username, int creditToDecrease) {
        return repository.findByUsername(username).thenCompose(optionalUser -> {
            if (optionalUser.isPresent()) {
                JsonObject user = optionalUser.get();
                int newCredit = Math.max(user.getInteger("credit") - creditToDecrease, 0);
                user.put("credit", newCredit);
                return repository.update(user).thenApply(v ->{
                    UserEventPublisher.publishUserUpdate(username, user);
                    UserEventPublisher.publishAllUsersUpdates(user);
                    return user;
                });
            }
            return CompletableFuture.completedFuture(null);
        });
    }

    @Override
    public CompletableFuture<JsonArray> getAllUsers() {
        return repository.findAll().thenApply(users -> {
            System.out.println("Users: " + users);
            return users;
        });
    }
}
