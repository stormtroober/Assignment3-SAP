package application;

import application.ports.*;
import domain.events.CreditRecharged;
import domain.events.CreditUpdated;
import domain.events.UserCreated;
import domain.events.UserEvent;
import domain.model.User;
import domain.model.UserType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class UserServiceEventSourcedImpl implements UserServiceAPI {
  private final EventStore eventStore;
  private final UserEventPublisher userEventPublisher;
  private final ConcurrentMap<String, User> cache = new ConcurrentHashMap<>();

  public UserServiceEventSourcedImpl(EventStore eventStore, UserEventPublisher userEventPublisher) {
    this.eventStore = eventStore;
    this.userEventPublisher = userEventPublisher;
  }

  private CompletableFuture<User> getOrLoad(String username) {
    User cached = cache.get(username);
    if (cached != null) {
      return CompletableFuture.completedFuture(cached);
    }

    return eventStore
        .loadEvents(username, 0)
        .thenApply(
            history -> {
              User agg = new User(history);
              cache.put(username, agg);
              return agg;
            });
  }

  @Override
  public CompletableFuture<User> signUp(String username, UserType type) {
    return getOrLoad(username)
        .thenCompose(
            agg -> {
              UserCreated evt;
              try {
                evt = agg.create(username, type.toString(), 100);
              } catch (IllegalStateException e) {
                CompletableFuture<User> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("User already exists"));
                return failed;
              }

              return eventStore
                  .appendEvent(username, evt, agg.getVersion())
                  .thenApply(
                      v -> {
                        agg.applyEvent(evt);
                        // Publish events for external communication
                        userEventPublisher.publishUserUpdate(username, agg);
                        userEventPublisher.publishAllUsersUpdates(agg);
                        return agg;
                      });
            });
  }

  @Override
  public CompletableFuture<User> signIn(String username) {
    return getOrLoad(username)
        .thenApply(
            agg -> {
              if (agg.getVersion() == 0) {
                System.out.println("User not found");
                return null;
              }
              userEventPublisher.publishAllUsersUpdates(agg);
              return agg;
            });
  }

  @Override
  public CompletableFuture<User> rechargeCredit(String username, int creditToAdd) {
    return getOrLoad(username)
        .thenCompose(
            agg -> {
              if (agg.getVersion() == 0) {
                return CompletableFuture.completedFuture(null);
              }

              CreditRecharged evt = agg.recharge(creditToAdd);
              return eventStore
                  .appendEvent(username, evt, agg.getVersion())
                  .thenApply(
                      v -> {
                        agg.applyEvent(evt);
                        userEventPublisher.publishUserUpdate(username, agg);
                        userEventPublisher.publishAllUsersUpdates(agg);
                        return agg;
                      });
            });
  }

  @Override
  public CompletableFuture<User> updateCredit(String username, int newCredit) {
    return getOrLoad(username)
        .thenCompose(
            agg -> {
              if (agg.getVersion() == 0) {
                CompletableFuture<User> failed = new CompletableFuture<>();
                failed.completeExceptionally(new RuntimeException("User not found"));
                return failed;
              }

              if (agg.getCredit() == newCredit) {
                return CompletableFuture.completedFuture(agg);
              }

              CreditUpdated evt = agg.updateCredit(newCredit);
              return eventStore
                  .appendEvent(username, evt, agg.getVersion())
                  .thenApply(
                      v -> {
                        agg.applyEvent(evt);
                        userEventPublisher.publishUserUpdate(username, agg);
                        userEventPublisher.publishAllUsersUpdates(agg);
                        return agg;
                      });
            });
  }

  @Override
  public CompletableFuture<List<User>> getAllUsers() {
    // Load all events and group by aggregateId to rebuild all users
    return eventStore
        .loadAllEvents()
        .thenApply(
            allEvents -> {
              // Group events by aggregateId
              Map<String, List<UserEvent>> eventsByUser =
                  allEvents.stream().collect(Collectors.groupingBy(UserEvent::getAggregateId));

              //JsonArray users = new JsonArray();
              List<User> users = new ArrayList<>();

              // Rebuild each user aggregate from their events
              eventsByUser.forEach(
                  (username, events) -> {
                    if (!events.isEmpty()) {
                      User agg = new User(events);
                      if (agg.getVersion() > 0) {
                        // Update cache with rebuilt aggregate
                        cache.put(username, agg);
                        users.add(agg);
                      }
                    }
                  });

              return users;
            });
  }
}
