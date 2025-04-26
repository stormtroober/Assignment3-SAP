package application.ports;

import domain.model.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/** Port for the User Service API. Provides methods to manage the Application domain. */
public interface UserServiceAPI {

  /**
   * Signs in a user with the given username.
   *
   * @param username the username of the user
   * @return a CompletableFuture containing the user details as a JsonObject if the sign-in is
   *     successful, or null if the sign-in fails
   */
  CompletableFuture<JsonObject> signIn(String username);

  /**
   * Signs up a new user with the given username.
   *
   * @param username the username of the user
   * @param type the type of the user
   * @return a CompletableFuture containing the created user details as a JsonObject
   */
  CompletableFuture<JsonObject> signUp(String username, User.UserType type);

  /**
   * Retrieves a user by its username.
   *
   * @param username the username of the user
   * @return a CompletableFuture containing an Optional with the user as a JsonObject if found, or
   *     an empty Optional if not found
   */
  CompletableFuture<Optional<JsonObject>> getUserByUsername(String username);

  /**
   * Updates the details of an existing user.
   *
   * @param user the user details to update as a JsonObject
   * @return a CompletableFuture containing the updated user as a JsonObject
   */
  CompletableFuture<JsonObject> updateUser(JsonObject user);

  /**
   * Recharges the credit of a user.
   *
   * @param username the username of the user
   * @param creditToAdd the amount of credit to add
   * @return a CompletableFuture containing the updated user as a JsonObject
   */
  CompletableFuture<JsonObject> rechargeCredit(String username, int creditToAdd);

  /**
   * Decreases the credit of a user.
   *
   * @param username the username of the user
   * @param creditToDecrease the amount of credit to decrease
   * @return a CompletableFuture containing the updated user as a JsonObject
   */
  CompletableFuture<JsonObject> decreaseCredit(String username, int creditToDecrease);

  /**
   * Retrieves all users.
   *
   * @return a CompletableFuture containing a JsonArray of all users
   */
  CompletableFuture<JsonArray> getAllUsers();
}
