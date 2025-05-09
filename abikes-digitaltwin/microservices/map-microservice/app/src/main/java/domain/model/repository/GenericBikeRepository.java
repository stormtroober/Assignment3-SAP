// file: domain/model/repository/GenericBikeRepository.java
package domain.model.repository;

import ddd.Repository;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface GenericBikeRepository<T> extends Repository {

  CompletableFuture<Void> saveBike(T bike);

  CompletableFuture<T> getBike(String bikeId);

  CompletableFuture<Void> assignBikeToUser(String username, T bike);

  CompletableFuture<Void> unassignBikeFromUser(String username, T bike);

  CompletableFuture<List<T>> getAvailableBikes();

  CompletableFuture<String> isBikeAssigned(T bike);

  CompletableFuture<Map<String, List<T>>> getUsersWithAssignedAndAvailableBikes();

  CompletableFuture<List<T>> getAllBikes();

  CompletableFuture<List<T>> getAllBikes(String username);
}
