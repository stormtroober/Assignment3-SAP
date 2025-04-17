package application.ports;

import io.vertx.core.json.JsonObject;

/**
 * Port representing an event publisher for user updates.
 */
public interface UserEventPublisher {

    /**
     * Publishes an update for a user.
     *
     * @param username the username of the user.
     * @param user a JsonObject representing the user update.
     */
    void publishUserUpdate(String username, JsonObject user);

    /**
     * Publishes updates for all users.
     *
     * @param users a JsonObject containing updates for all users.
     */
    void publishAllUsersUpdates(JsonObject users);

}