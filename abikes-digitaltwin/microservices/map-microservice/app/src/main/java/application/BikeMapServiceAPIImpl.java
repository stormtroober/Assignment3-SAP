package application;

import application.ports.BikeMapServiceAPI;
import application.ports.EventPublisher;
import application.ports.StationMapServiceAPI;
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
  private final StationMapServiceAPI stationMapService;

  public BikeMapServiceAPIImpl(EventPublisher eventPublisher, StationMapServiceAPI stationMapService ) {
    this.aBikeRepository = new ABikeRepositoryImpl();
    this.eBikeRepository = new EBikeRepositoryImpl();
    this.eventPublisher = eventPublisher;
    this.stationMapService = stationMapService;
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
        System.out.println("Starting updateABike for bike: " + bike.getId());
        return aBikeRepository
                .saveBike(bike)
                .thenAccept(
                        v -> {
                            System.out.println("Successfully saved ABike: " + bike.getId());

                            // publish full fleet update to ADMIN channel
                            aBikeRepository.getAllBikes().thenAccept(bikes -> {
                                System.out.println("Publishing full ABike fleet update with " + bikes.size() + " bikes");
                                eventPublisher.publishABikesUpdate(bikes);
                            });

                            // publish per-user assigned + available bikes (USER channel)
                            aBikeRepository
                                    .getUsersWithAssignedAndAvailableBikes()
                                    .thenAccept(
                                            usersWithBikeMap -> {
                                                System.out.println("Retrieved users with assigned bikes map, size: " + usersWithBikeMap.size());
                                                if (!usersWithBikeMap.isEmpty()) {
                                                    usersWithBikeMap.forEach(
                                                            (user, userBikes) -> {
                                                                System.out.println("Publishing ABikes update for user " + user + " with " + userBikes.size() + " bikes");
                                                                eventPublisher.publishUserABikesUpdate(userBikes, user);
                                                            });

                                                    registeredUsers.stream()
                                                            .filter(user -> !usersWithBikeMap.containsKey(user))
                                                            .forEach(
                                                                    user ->
                                                                            aBikeRepository
                                                                                    .getAvailableBikes()
                                                                                    .thenAccept(
                                                                                            avail -> {
                                                                                                System.out.println("Publishing available ABikes to user " + user + " with " + avail.size() + " bikes");
                                                                                                eventPublisher.publishUserABikesUpdate(avail, user);
                                                                                            }));
                                                } else {
                                                    // no one has a bike, just broadcast available
                                                    System.out.println("No users with assigned bikes, broadcasting available bikes to all");
                                                    aBikeRepository
                                                            .getAvailableBikes()
                                                            .thenAccept(available -> {
                                                                System.out.println("Broadcasting " + available.size() + " available ABikes to all users");
                                                                eventPublisher.publishUserAvailableABikesUpdate(available);
                                                            });
                                                }
                                            });

                            // publish PUBLIC channel update (all USERS) channel
                            aBikeRepository.getPublicBikes().thenAccept(publicBikes -> {
                                if (!publicBikes.isEmpty()) {
                                    System.out.println("Publishing public ABikes update with " + publicBikes.size() + " bikes");
                                    eventPublisher.publishPublicABikesUpdate(publicBikes);
                                } else {
                                    System.out.println("No public ABikes to publish.");
                                }
                            });
                        })
                .exceptionally(ex -> {
                    System.err.println("Error in updateABike: " + ex.getMessage());
                    ex.printStackTrace();
                    return null;
                });
    }



    @Override
    public CompletableFuture<Void> notifyStartRide(String username, String bikeName, BikeType bikeType) {
        if (bikeType == BikeType.NORMAL) {
            return eBikeRepository
                    .getBike(bikeName)
                    .thenCompose(bike -> eBikeRepository.assignBikeToUser(username, bike))
                    .thenAccept(v -> {
                        eBikeRepository
                                .getAvailableBikes()
                                .thenAccept(eventPublisher::publishUserAvailableEBikesUpdate);
                    });
        } else if (bikeType == BikeType.AUTONOMOUS) {
            return aBikeRepository
                    .getBike(bikeName)
                    .thenCompose(bike -> aBikeRepository.assignBikeToUser(username, bike))
                    .thenAccept(v -> {
                        aBikeRepository
                                .getAvailableBikes()
                                .thenAccept(eventPublisher::publishUserAvailableABikesUpdate);
                    });
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
                eventPublisher.publishStopRide(username, bikeType);
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
                eventPublisher.publishStopRide(username, bikeType);
              });
    } else {
      return CompletableFuture.failedFuture(
          new IllegalArgumentException("Unsupported bike type: " + bikeType));
    }
  }

    @Override
    public CompletableFuture<Void> notifyStartPublicRide(String bikeName, BikeType bikeType) {
         if (bikeType == BikeType.AUTONOMOUS) {
            return aBikeRepository
                    .getBike(bikeName)
                    .thenCompose(aBikeRepository::assignBikeToPublic);
        } else {
            return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Unsupported bike type: " + bikeType));
        }
    }

    @Override
    public CompletableFuture<Void> notifyStopPublicRide(String bikeName, BikeType bikeType) {
        if (bikeType == BikeType.AUTONOMOUS) {
            return aBikeRepository
                    .getBike(bikeName)
                    .thenCompose(aBikeRepository::unassignBikeFromPublic);
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
