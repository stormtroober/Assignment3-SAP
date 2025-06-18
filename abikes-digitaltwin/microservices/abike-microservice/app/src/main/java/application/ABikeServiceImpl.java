package application;

import application.ports.ABikeRepository;
import application.ports.ABikeServiceAPI;
import application.ports.BikeCommunicationPort;
import application.ports.StationServiceAPI;
import domain.model.*;
import io.vertx.core.json.JsonObject;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ABikeServiceImpl implements ABikeServiceAPI {

  private final ABikeRepository repository;
  private final BikeCommunicationPort bikeCommunicationAdapter;
  private final StationServiceAPI stationService;
  private final Random random = new Random();
  public static final Integer MAX_BATTERY = 300;
  private static final Logger logger = LoggerFactory.getLogger(ABikeServiceImpl.class);

  public ABikeServiceImpl(
      ABikeRepository repository,
      BikeCommunicationPort bikeCommunicationAdapter,
      StationServiceAPI stationService) {
    this.repository = repository;
    this.bikeCommunicationAdapter = bikeCommunicationAdapter;
    this.stationService = stationService;
    repository.findAll().thenAccept(bikeCommunicationAdapter::sendAllUpdates);
  }

  // Get all stations and pick a random one for the bike's location
  @Override
  public CompletableFuture<JsonObject> createABike(String id) {
    return stationService
        .findStationWithFreeSlot()
        .thenCompose(
            optStation ->
                optStation
                    .map(
                        station ->
                            stationService
                                .assignBikeToStation(station.getString("id"), id)
                                .thenApply(updated -> updated.getJsonObject("location")))
                    .orElseGet(
                        () ->
                            CompletableFuture.completedFuture(
                                new JsonObject()
                                    .put("x", 100 * random.nextDouble())
                                    .put("y", 100 * random.nextDouble()))))
        .thenCompose(
            location -> {
              ABike abike =
                  ABikeFactory.getInstance()
                      .create(
                          id,
                          new P2d(location.getDouble("x"), location.getDouble("y")),
                          ABikeState.AVAILABLE,
                          MAX_BATTERY,
                          BikeType.AUTONOMOUS);
              //TODO, to be removed when communication uses domain model
              bikeCommunicationAdapter.sendUpdate(ABikeMapper.toJson(abike));
              return repository.save(abike).thenApply(v -> ABikeMapper.toJson(abike));
            });
  }

  @Override
  public CompletableFuture<Optional<ABike>> getABike(String id) {
    return repository.findById(id);
  }

  @Override
  public CompletableFuture<JsonObject> rechargeABike(String id) {
    return repository
            .findById(id)
            .thenCompose(optionalABike -> {
              if (optionalABike.isPresent()) {
                ABike abike = optionalABike.get();
                ABike recharged = new ABike(
                        abike.getId(),
                        abike.getLocation(),
                        ABikeState.AVAILABLE,
                        MAX_BATTERY,
                        abike.getType()
                );
                //TODO: remove when communication uses domain model
                JsonObject abikeJson = ABikeMapper.toJson(recharged);
                bikeCommunicationAdapter.sendUpdate(abikeJson);
                return repository.update(recharged).thenApply(v -> abikeJson);
              }
              return CompletableFuture.completedFuture(null);
            });
  }
  @Override
  public CompletableFuture<JsonObject> updateABike(ABike abike) {
    ABike updatedABike;
    if (abike.getBatteryLevel() == 0 && abike.getABikeState() != ABikeState.MAINTENANCE) {
      updatedABike = new ABike(
        abike.getId(),
        abike.getLocation(),
        ABikeState.MAINTENANCE,
        abike.getBatteryLevel(),
        abike.getType()
      );
    } else {
        updatedABike = abike;
    }
    return repository
        .update(updatedABike)
        .thenCompose(
            v -> repository
                .findById(updatedABike.getId())
                .thenApply(opt -> {
                    //todo: remove when communication uses domain model
                  bikeCommunicationAdapter.sendUpdate(ABikeMapper.toJson(updatedABike));
                  return ABikeMapper.toJson(updatedABike);
                })
        );
  }

  @Override
  public CompletableFuture<List<ABike>> getAllABikes() {
    return repository.findAll();
  }
}
