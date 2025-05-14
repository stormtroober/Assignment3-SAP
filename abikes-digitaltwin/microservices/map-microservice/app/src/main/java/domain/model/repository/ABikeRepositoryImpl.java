package domain.model.repository;

import ddd.Repository;
import domain.model.ABike;
import domain.model.ABikeState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class ABikeRepositoryImpl implements ABikeRepository, Repository {
    Logger logger = LoggerFactory.getLogger(ABikeRepositoryImpl.class);
  private final ConcurrentHashMap<String, ABike> bikes = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, String> bikeAssignments = new ConcurrentHashMap<>();

  @Override
  public CompletableFuture<Void> saveBike(ABike bike) {
    bikes.put(bike.getId(), bike);
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public CompletableFuture<ABike> getBike(String bikeName) {
    ABike bike = bikes.get(bikeName);
    if (bike != null) {
      return CompletableFuture.completedFuture(bike);
    } else {
      return CompletableFuture.failedFuture(new IllegalArgumentException("Bike not found"));
    }
  }

  @Override
  public CompletableFuture<Void> assignBikeToUser(String username, ABike bike) {
    return CompletableFuture.runAsync(
        () -> {
          if (!bikes.containsKey(bike.getId())) {
            throw new IllegalArgumentException("Bike not found in repository");
          }
          if (bikeAssignments.containsValue(bike.getId())) {
            throw new IllegalStateException("Bike is already assigned");
          }
            logger.info("Assigning bike {} to user {}", bike, username);
          bikeAssignments.put(username, bike.getId());
        });
  }

  @Override
  public CompletableFuture<Void> unassignBikeFromUser(String username, ABike bike) {
    return CompletableFuture.runAsync(
        () -> {
          if (!bikeAssignments.containsKey(username)) {
            throw new IllegalArgumentException("User has no bike assigned");
          }
          if (!bikeAssignments.get(username).equals(bike.getId())) {
            throw new IllegalArgumentException("Bike is not assigned to user");
          }
          bikeAssignments.remove(username);
        });
  }

  @Override
  public CompletableFuture<List<ABike>> getAvailableBikes() {
    return CompletableFuture.supplyAsync(
        () ->
            bikes.values().stream()
                .filter(b -> b.getState() == ABikeState.AVAILABLE)
                .collect(Collectors.toList()));
  }

  @Override
  public CompletableFuture<String> isBikeAssigned(ABike bike) {
    return CompletableFuture.supplyAsync(
        () -> {
          return bikeAssignments.entrySet().stream()
              .filter(e -> e.getValue().equals(bike.getId()))
              .map(Map.Entry::getKey)
              .findFirst()
              .orElse(null);
        });
  }

  @Override
  public CompletableFuture<Map<String, List<ABike>>> getUsersWithAssignedAndAvailableBikes() {
    return CompletableFuture.supplyAsync(
        () -> {
          List<ABike> available =
              bikes.values().stream().filter(b -> b.getState() == ABikeState.AVAILABLE).toList();

          return bikeAssignments.entrySet().stream()
              .collect(
                  Collectors.toMap(
                      Map.Entry::getKey,
                      e -> {
                        List<ABike> userBikes =
                            bikes.values().stream()
                                .filter(b -> b.getId().equals(e.getValue()))
                                .collect(Collectors.toList());
                        userBikes.addAll(available);
                        return userBikes;
                      }));
        });
  }

  @Override
  public CompletableFuture<List<ABike>> getAllBikes() {
    return CompletableFuture.supplyAsync(() -> new ArrayList<>(bikes.values()));
  }

  @Override
  public CompletableFuture<List<ABike>> getAllBikes(String username) {
    return CompletableFuture.supplyAsync(
        () ->
            bikes.values().stream()
                .filter(b -> username.equals(bikeAssignments.get(username)))
                .collect(Collectors.toList()));
  }
}
