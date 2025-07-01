package application.ports;

import domain.model.ABike;
import java.util.concurrent.CompletableFuture;

/** Service API for managing ABike domain operations. */
public interface ABikeServiceAPI {

  /**
   * Creates a new ABike with the given ID.
   *
   * @param id the unique identifier for the ABike
   * @return a CompletableFuture containing the created ABike
   */
  CompletableFuture<ABike> createABike(String id);

  /**
   * Recharges the ABike with the specified ID.
   *
   * @param id the unique identifier of the ABike to recharge
   * @return a CompletableFuture containing the recharged ABike
   */
  CompletableFuture<ABike> rechargeABike(String id);

  /**
   * Updates the given ABike's information.
   *
   * @param abike the ABike object with updated data
   * @return a CompletableFuture containing the updated ABike
   */
  CompletableFuture<ABike> updateABike(ABike abike);
}
