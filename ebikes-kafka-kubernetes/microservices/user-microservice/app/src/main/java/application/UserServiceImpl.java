package application;

import application.ports.EventStore;
import application.ports.UserEventPublisher;
import application.ports.UserRepository;
import application.ports.UserServiceAPI;
import domain.events.CreditRecharged;
import domain.events.CreditUpdated;
import domain.events.UserCreated;
import domain.model.User;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.CompletableFuture;

public class UserServiceImpl implements UserServiceAPI {

  private final UserRepository repository;
  private final UserEventPublisher userEventPublisher;
  private final EventStore eventStore;

  public UserServiceImpl(UserRepository repository, UserEventPublisher userEventPublisher, EventStore eventStore) {
    this.repository = repository;
    this.userEventPublisher = userEventPublisher;
    this.eventStore = eventStore;
  }

    @Override
    public CompletableFuture<JsonObject> signIn(String username) {
        return repository
                .findByUsername(username)
                .thenApply(
                        user -> {
                            if (user.isPresent()) {
                                JsonObject userJson = user.get();
                                userEventPublisher.publishAllUsersUpdates(userJson);
                                return userJson;
                            } else {
                                System.out.println("User not found");
                            }
                            return null;
                        });
    }

    @Override
    public CompletableFuture<JsonObject> signUp(String username, User.UserType type) {
        return repository
                .findByUsername(username)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isPresent()) {
                        CompletableFuture<JsonObject> f = new CompletableFuture<>();
                        f.completeExceptionally(new RuntimeException("User already exists"));
                        return f;
                    }

                    int credit = 100;
                    JsonObject user = new JsonObject()
                            .put("username", username)
                            .put("type", type.toString())
                            .put("credit", credit);

                    return repository
                            .save(user)
                            .thenCompose(v -> {
                                // 1) salva l'evento nel log
                                return eventStore
                                        .loadEvents(username, 0)                           // carica la lista corrente
                                        .thenCompose(history -> {
                                            long nextSeq = history.size() + 1;
                                            UserCreated evt = new UserCreated(
                                                    username,
                                                    nextSeq,
                                                    type.toString(),
                                                    credit
                                            );
                                            return eventStore.appendEvent(username, evt, history.size());
                                        })
                                        // 2) pubblica e rispondi
                                        .thenApply(x -> {
                                            userEventPublisher.publishUserUpdate(username, user);
                                            userEventPublisher.publishAllUsersUpdates(user);
                                            return user;
                                        });
                            });
                });
    }

    @Override
    public CompletableFuture<JsonObject> rechargeCredit(String username, int creditToAdd) {
        return repository
                .findByUsername(username)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        return CompletableFuture.completedFuture(null);
                    }
                    JsonObject user = optionalUser.get();
                    int newCredit = user.getInteger("credit") + creditToAdd;
                    user.put("credit", newCredit);

                    return repository.update(user)
                            .thenCompose(v -> {
                                // append evento
                                return eventStore.loadEvents(username, 0)
                                        .thenCompose(history -> {
                                            long nextSeq = history.size() + 1;
                                            CreditRecharged evt = new CreditRecharged(username, nextSeq, creditToAdd);
                                            return eventStore.appendEvent(username, evt, history.size());
                                        })
                                        .thenApply(x -> {
                                            userEventPublisher.publishUserUpdate(username, user);
                                            userEventPublisher.publishAllUsersUpdates(user);
                                            return user;
                                        });
                            });
                });
    }


    @Override
    public CompletableFuture<JsonObject> updateCredit(String username, int newCredit) {
        return repository
                .findByUsername(username)
                .thenCompose(optionalUser -> {
                    if (optionalUser.isEmpty()) {
                        CompletableFuture<JsonObject> f = new CompletableFuture<>();
                        f.completeExceptionally(new RuntimeException("User not found"));
                        return f;
                    }

                    JsonObject user = optionalUser.get();
                    int currentCredit = user.getInteger("credit", 0);
                    
                    if (currentCredit == newCredit) {
                        // No change needed
                        return CompletableFuture.completedFuture(user);
                    }

                    user.put("credit", newCredit);

                    return repository.update(user)
                            .thenCompose(v ->
                                    eventStore.loadEvents(username, 0)
                            )
                            .thenCompose(history -> {
                                long nextSeq = history.size() + 1;
                                CreditUpdated evt = new CreditUpdated(username, nextSeq, newCredit);
                                return eventStore.appendEvent(username, evt, history.size());
                            })
                            .thenApply(x -> {
                                userEventPublisher.publishUserUpdate(username, user);
                                userEventPublisher.publishAllUsersUpdates(user);
                                return user;
                            });
                });
    }

  @Override
  public CompletableFuture<JsonArray> getAllUsers() {
    return repository
        .findAll()
        .thenApply(
            users -> {
              System.out.println("Users: " + users);
              return users;
            });
  }
}
