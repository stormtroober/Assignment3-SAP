package application;

import application.ports.BikeMapServiceAPI;
import application.ports.EventPublisher;
import domain.model.ABike;
import domain.model.BikeType;
import domain.model.EBike;
import domain.model.repository.ABikeRepository;
import domain.model.repository.ABikeRepositoryImpl;
import domain.model.repository.EBikeRepository;
import domain.model.repository.EBikeRepositoryImpl;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class BikeMapServiceAPIImpl implements BikeMapServiceAPI {

  private final ABikeRepository aBikeRepository;
  private final EBikeRepository eBikeRepository;
  private final EventPublisher eventPublisher;
  private final List<String> registeredUsers = new CopyOnWriteArrayList<>();

  public BikeMapServiceAPIImpl(EventPublisher eventPublisher) {
    this.aBikeRepository = new ABikeRepositoryImpl();
    this.eBikeRepository = new EBikeRepositoryImpl();
    this.eventPublisher = eventPublisher;
  }

  @Override
  public CompletableFuture<Void> updateEBike(EBike bike) {

    return eBikeRepository
        .saveBike(bike)
        .thenAccept(
            v -> {
              eBikeRepository.getAllBikes().thenAccept(eventPublisher::publishBikesUpdate);

              eBikeRepository
                  .getUsersWithAssignedAndAvailableBikes()
                  .thenAccept(
                      usersWithBikeMap -> {
                        if (!usersWithBikeMap.isEmpty()) {
                          usersWithBikeMap.forEach(
                              (username, userBikes) ->
                                  eventPublisher.publishUserEBikesUpdate(userBikes, username));

                          registeredUsers.stream()
                              .filter(
                                  user ->
                                      !usersWithBikeMap.containsKey(
                                          user)) // Filter users without bikes assigned
                              .forEach(
                                  user ->
                                      eBikeRepository
                                          .getAvailableBikes()
                                          .thenAccept(
                                              availableBikes -> {
                                                eventPublisher.publishUserEBikesUpdate(
                                                    availableBikes, user);
                                              }));
                        } else {
                          eBikeRepository
                              .getAvailableBikes()
                              .thenAccept(eventPublisher::publishUserAvailableEBikesUpdate);
                        }
                      });
            });
  }

  @Override
  public CompletableFuture<Void> updateABike(ABike bike) {
    return aBikeRepository
        .saveBike(bike)
        .thenAccept(
            v -> {
              // publish full fleet update
              aBikeRepository.getAllBikes().thenAccept(eventPublisher::publishABikesUpdate);

              // publish per-user assigned + available bikes
              aBikeRepository
                  .getUsersWithAssignedAndAvailableBikes()
                  .thenAccept(
                      usersWithBikeMap -> {
                        if (!usersWithBikeMap.isEmpty()) {
                          usersWithBikeMap.forEach(
                              (user, userBikes) ->
                                  eventPublisher.publishUserABikesUpdate(userBikes, user));

                          registeredUsers.stream()
                              .filter(user -> !usersWithBikeMap.containsKey(user))
                              .forEach(
                                  user ->
                                      aBikeRepository
                                          .getAvailableBikes()
                                          .thenAccept(
                                              avail ->
                                                  eventPublisher.publishUserABikesUpdate(
                                                      avail, user)));
                        } else {
                          // no one has a bike, just broadcast available
                          aBikeRepository
                              .getAvailableBikes()
                              .thenAccept(eventPublisher::publishUserAvailableABikesUpdate);
                        }
                      });
            });
  }

  @Override
  public CompletableFuture<Void> notifyStartRide(
      String username, String bikeName, BikeType bikeType) {
    if (bikeType == BikeType.NORMAL) {
      return eBikeRepository
          .getBike(bikeName)
          .thenCompose(bike -> eBikeRepository.assignBikeToUser(username, bike))
          .thenAccept(
              v ->
                  eBikeRepository
                      .getAvailableBikes()
                      .thenAccept(eventPublisher::publishUserAvailableEBikesUpdate));
    } else if (bikeType == BikeType.AUTONOMOUS) {
      return aBikeRepository
          .getBike(bikeName)
          .thenCompose(bike -> aBikeRepository.assignBikeToUser(username, bike))
          .thenAccept(
              v ->
                  aBikeRepository
                      .getAvailableBikes()
                      .thenAccept(eventPublisher::publishUserAvailableABikesUpdate));
    } else {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Unsupported bike type: " + bikeType));
    }
  }

  @Override
  public CompletableFuture<Void> notifyStopRide(
      String username, String bikeName, BikeType bikeType) {
    if (bikeType == BikeType.NORMAL) {
      return eBikeRepository
          .getBike(bikeName)
          .thenCompose(bike -> eBikeRepository.unassignBikeFromUser(username, bike))
          .thenAccept(
              v -> {
                eBikeRepository
                    .getAvailableBikes()
                    .thenAccept(eventPublisher::publishUserAvailableEBikesUpdate);
                eventPublisher.publishStopRide(username);
              });
    } else if (bikeType == BikeType.AUTONOMOUS) {
      return aBikeRepository
          .getBike(bikeName)
          .thenCompose(bike -> aBikeRepository.unassignBikeFromUser(username, bike))
          .thenAccept(
              v -> {
                aBikeRepository
                    .getAvailableBikes()
                    .thenAccept(eventPublisher::publishUserAvailableABikesUpdate);
                eventPublisher.publishStopRide(username);
              });
    } else {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Unsupported bike type: " + bikeType));
    }
  }

  @Override
  public void getAllBikes() {
    eBikeRepository.getAllBikes().thenAccept(eventPublisher::publishBikesUpdate);
    aBikeRepository.getAllBikes().thenAccept(eventPublisher::publishABikesUpdate);
  }

  @Override
  public void getAllBikes(String username) {
    List<EBike> availableBikes = eBikeRepository.getAvailableBikes().join();
    List<ABike> availableABikes = aBikeRepository.getAvailableBikes().join();
    List<EBike> userBikes = eBikeRepository.getAllBikes(username).join();
    List<ABike> userABikes = aBikeRepository.getAllBikes(username).join();

    if (!userBikes.isEmpty()) {
      availableBikes.addAll(userBikes);
      eventPublisher.publishUserEBikesUpdate(availableBikes, username);
    } else {
      System.out.println("No bikes assigned to user: " + username);
      System.out.println("Available bikes: " + availableBikes);
      eventPublisher.publishUserAvailableEBikesUpdate(availableBikes);
    }

    if (!userABikes.isEmpty()) {
      availableABikes.addAll(userABikes);
      eventPublisher.publishUserABikesUpdate(availableABikes, username);
    } else {
      System.out.println("No bikes assigned to user: " + username);
      System.out.println("Available bikes: " + availableABikes);
      eventPublisher.publishUserAvailableABikesUpdate(availableABikes);
    }
  }

  @Override
  public void registerUser(String username) {
    registeredUsers.add(username);
  }

  @Override
  public void deregisterUser(String username) {
    registeredUsers.remove(username);
  }
}
