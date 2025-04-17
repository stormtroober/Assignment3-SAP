package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Port for the User Repository.
 * Provides methods to perform CRUD operations on users.
 */
public interface UserRepository{

    /**
     * Saves a new user.
     *
     * @param user the user details to save as a JsonObject
     * @return a CompletableFuture that completes when the save operation is done
     */
    CompletableFuture<Void> save(JsonObject user);

    /**
     * Updates an existing user.
     *
     * @param user the user details to update as a JsonObject
     * @return a CompletableFuture that completes when the update operation is done
     */
    CompletableFuture<Void> update(JsonObject user);

    /**
     * Finds a user by its username.
     *
     * @param username the username of the user
     * @return a CompletableFuture containing an Optional with the user as a JsonObject if found, or an empty Optional if not found
     */
    CompletableFuture<Optional<JsonObject>> findByUsername(String username);

    /**
     * Retrieves all users.
     *
     * @return a CompletableFuture containing a JsonArray of all users
     */
    CompletableFuture<JsonArray> findAll();
}