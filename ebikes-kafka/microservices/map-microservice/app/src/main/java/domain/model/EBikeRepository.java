package domain.model;

import java.util.Map;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Port representing the repository for managing e-bikes.
 */
public interface EBikeRepository{

    /**
     * Saves an e-bike to the repository.
     *
     * @param bike the e-bike to save.
     * @return a CompletableFuture that completes when the bike is saved.
     */
    CompletableFuture<Void> saveBike(EBike bike);

    /**
     * Retrieves an e-bike from the repository by its name.
     *
     * @param bikeName the name of the e-bike to retrieve.
     * @return a CompletableFuture containing the retrieved e-bike.
     */
    CompletableFuture<EBike> getBike(String bikeName);

    /**
     * Assigns an e-bike to a user.
     *
     * @param username the username of the user.
     * @param bike the e-bike to assign.
     * @return a CompletableFuture that completes when the bike is assigned.
     */
    CompletableFuture<Void> assignBikeToUser(String username, EBike bike);

    /**
     * Unassigns an e-bike from a user.
     *
     * @param username the username of the user.
     * @param bike the e-bike to unassign.
     * @return a CompletableFuture that completes when the bike is unassigned.
     */
    CompletableFuture<Void> unassignBikeFromUser(String username, EBike bike);

    /**
     * Retrieves a list of all available e-bikes.
     *
     * @return a CompletableFuture containing a list of available e-bikes.
     */
    CompletableFuture<List<EBike>> getAvailableBikes();

    /**
     * Checks if an e-bike is assigned to a user.
     *
     * @param bike the e-bike to check.
     * @return a CompletableFuture containing the username of the user the bike is assigned to, or null if not assigned.
     */
    CompletableFuture<String> isBikeAssigned(EBike bike);

    /**
     * Retrieves a map of users with their assigned and available e-bikes.
     *
     * @return a CompletableFuture containing a map of usernames to lists of assigned and available e-bikes.
     */
    CompletableFuture<Map<String, List<EBike>>> getUsersWithAssignedAndAvailableBikes();

    /**
     * Retrieves a list of all e-bikes.
     *
     * @return a CompletableFuture containing a list of all e-bikes.
     */
    CompletableFuture<List<EBike>> getAllBikes();

    /**
     * Retrieves a list of all e-bikes assigned to a specific user.
     *
     * @param username the username of the user.
     * @return a CompletableFuture containing a list of e-bikes assigned to the user.
     */
    CompletableFuture<List<EBike>> getAllBikes(String username);
}