package application;

import application.ports.*;
import domain.events.CreditRecharged;
import domain.events.CreditUpdated;
import domain.events.UserCreated;
import domain.model.User;
import domain.model.UserAggregate;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import java.util.concurrent.*;

public class UserServiceEventSourcedImpl implements UserServiceAPI {
    private final EventStore            eventStore;
    private final UserEventPublisher    userEventPublisher;
    private final UserRepository        readRepo;
    private final ConcurrentMap<String, UserAggregate> cache = new ConcurrentHashMap<>();

    public UserServiceEventSourcedImpl(EventStore eventStore,
                                       UserEventPublisher userEventPublisher,
                                       UserRepository readRepo) {
        this.eventStore = eventStore;
        this.userEventPublisher = userEventPublisher;
        this.readRepo = readRepo;
    }

    private CompletableFuture<UserAggregate> getOrLoad(String username) {
        UserAggregate cached = cache.get(username);
        if (cached != null) {
            return CompletableFuture.completedFuture(cached);
        }
        
        return eventStore
                .loadEvents(username, 0)
                .thenApply(history -> {
                    UserAggregate agg = new UserAggregate(history);
                    cache.put(username, agg);
                    return agg;
                });
    }

    @Override
    public CompletableFuture<JsonObject> signUp(String username, User.UserType type) {
        return getOrLoad(username)
                .thenCompose(agg -> {
                    UserCreated evt;
                    try {
                        evt = agg.create(username, type.toString(), 100);
                    } catch (IllegalStateException e) {
                        CompletableFuture<JsonObject> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new RuntimeException("User already exists"));
                        return failed;
                    }
                    
                    return eventStore.appendEvent(username, evt, agg.getVersion())
                            .thenApply(v -> {
                                agg.applyEvent(evt);
                                JsonObject userJson = agg.toJson();
                                // Update read model
                                readRepo.save(userJson);
                                // Publish events like in UserServiceImpl
                                userEventPublisher.publishUserUpdate(username, userJson);
                                userEventPublisher.publishAllUsersUpdates(userJson);
                                return userJson;
                            });
                });
    }

    @Override
    public CompletableFuture<JsonObject> signIn(String username) {
        return getOrLoad(username)
                .thenApply(agg -> {
                    if (agg.getVersion() == 0) {
                        System.out.println("User not found");
                        return null;
                    }
                    JsonObject userJson = agg.toJson();
                    userEventPublisher.publishAllUsersUpdates(userJson);
                    return userJson;
                });
    }

    @Override
    public CompletableFuture<JsonObject> rechargeCredit(String username, int creditToAdd) {
        return getOrLoad(username)
                .thenCompose(agg -> {
                    if (agg.getVersion() == 0) {
                        return CompletableFuture.completedFuture(null);
                    }
                    
                    CreditRecharged evt = agg.recharge(creditToAdd);
                    return eventStore.appendEvent(username, evt, agg.getVersion())
                            .thenApply(v -> {
                                agg.applyEvent(evt);
                                JsonObject userJson = agg.toJson();
                                readRepo.update(userJson);
                                userEventPublisher.publishUserUpdate(username, userJson);
                                userEventPublisher.publishAllUsersUpdates(userJson);
                                return userJson;
                            });
                });
    }

    @Override
    public CompletableFuture<JsonObject> updateCredit(String username, int newCredit) {
        return getOrLoad(username)
                .thenCompose(agg -> {
                    if (agg.getVersion() == 0) {
                        CompletableFuture<JsonObject> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new RuntimeException("User not found"));
                        return failed;
                    }
                    
                    if (agg.getCredit() == newCredit) {
                        return CompletableFuture.completedFuture(agg.toJson());
                    }
                    
                    CreditUpdated evt = agg.updateCredit(newCredit);
                    return eventStore.appendEvent(username, evt, agg.getVersion())
                            .thenApply(v -> {
                                agg.applyEvent(evt);
                                JsonObject userJson = agg.toJson();
                                readRepo.update(userJson);
                                userEventPublisher.publishUserUpdate(username, userJson);
                                userEventPublisher.publishAllUsersUpdates(userJson);
                                return userJson;
                            });
                });
    }

    @Override
    public CompletableFuture<JsonArray> getAllUsers() {
        return readRepo.findAll();
    }
}
