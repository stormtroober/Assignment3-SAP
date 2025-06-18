package application.ports;

import domain.model.ABike;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ABikeServiceAPI {
  CompletableFuture<JsonObject> createABike(String id);

  CompletableFuture<Optional<ABike>> getABike(String id);

  CompletableFuture<JsonObject> rechargeABike(String id);

  CompletableFuture<JsonObject> updateABike(ABike abike);

  CompletableFuture<List<ABike>> getAllABikes();
}
