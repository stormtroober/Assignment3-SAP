package application.ports;

import domain.model.ABike;

import java.util.concurrent.CompletableFuture;

public interface ABikeServiceAPI {
  CompletableFuture<ABike> createABike(String id);

  CompletableFuture<ABike> rechargeABike(String id);

  CompletableFuture<ABike> updateABike(ABike abike);
}
