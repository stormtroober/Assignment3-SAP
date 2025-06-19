package application.ports;

import domain.model.Station;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public interface StationRepository {
  CompletableFuture<Void> save(Station station);

  CompletableFuture<Void> update(Station station);

  CompletableFuture<Optional<Station>> findById(String id);

  CompletableFuture<List<Station>> findAll();
}
