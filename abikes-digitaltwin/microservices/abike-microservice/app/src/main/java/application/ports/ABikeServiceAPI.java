package application.ports;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface ABikeServiceAPI {
  CompletableFuture<JsonObject> createABike(String id);

  CompletableFuture<Optional<JsonObject>> getABike(String id);

  CompletableFuture<JsonObject> rechargeABike(String id);

  CompletableFuture<JsonObject> updateABike(JsonObject abike);

  CompletableFuture<JsonArray> getAllABikes();

}
