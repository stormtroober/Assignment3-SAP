package infrastructure.persistence;

import application.ports.EventStore;
import domain.events.CreditUpdated;
import domain.events.UserCreated;
import domain.events.CreditRecharged;
import domain.events.UserEvent;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MongoEventStore implements EventStore {
    private static final String COLLECTION = "user-events";
    private final MongoClient mongo;

    public MongoEventStore(MongoClient mongoClient) {
        this.mongo = mongoClient;
    }

    @Override
    public CompletableFuture<Void> appendEvent(
            String aggregateId,
            UserEvent event,
            long expectedVersion
    ) {
        CompletableFuture<Void> fut = new CompletableFuture<>();

        // Build the base document
        JsonObject doc = new JsonObject()
                .put("aggregateId", aggregateId)
                .put("sequence",    event.getSequence())
                .put("type",        event.getType())
                .put("occurredAt",  event.getOccurredAt());

        // Build the payload based on event type
        JsonObject payload = new JsonObject();
        switch (event.getType()) {
            case "UserCreated":
                UserCreated uc = (UserCreated) event;
                payload
                        .put("userType",      uc.getUserType())
                        .put("initialCredit", uc.getInitialCredit());
                break;

            case "CreditRecharged":
                CreditRecharged cr = (CreditRecharged) event;
                payload.put("newCredit", cr.getAmount());
                break;

            case "CreditUpdated":
                CreditUpdated cu = (CreditUpdated) event;
                payload.put("newCredit", cu.getNewCredit());
                break;

            default:
                fut.completeExceptionally(
                        new IllegalArgumentException("Unknown event type: " + event.getType())
                );
                return fut;
        }

        doc.put("payload", payload);

        // Insert into Mongo; rely on a unique index on (aggregateId, sequence)
        mongo.insert(COLLECTION, doc)
                .onSuccess(__ -> fut.complete(null))
                .onFailure(err -> fut.completeExceptionally(
                        new RuntimeException("Failed to append event: " + err.getMessage(), err)
                ));

        return fut;
    }

    @Override
    public CompletableFuture<List<UserEvent>> loadEvents(
            String aggregateId,
            long fromSequence
    ) {
        CompletableFuture<List<UserEvent>> fut = new CompletableFuture<>();

        JsonObject query = new JsonObject()
                .put("aggregateId", aggregateId)
                .put("sequence", new JsonObject().put("$gte", fromSequence));

        mongo.find(COLLECTION, query)
                .onSuccess(results -> {
                    // Sort by sequence ascending
                    results.sort(Comparator.comparingInt(d -> d.getInteger("sequence")));

                    List<UserEvent> history = new ArrayList<>();
                    for (JsonObject doc : results) {
                        String type = doc.getString("type");
                        long   seq  = doc.getLong("sequence");
                        JsonObject p = doc.getJsonObject("payload", new JsonObject());

                        switch (type) {
                            case "UserCreated":
                                history.add(new UserCreated(
                                        aggregateId,
                                        seq,
                                        p.getString("userType"),
                                        p.getInteger("initialCredit")
                                ));
                                break;

                            case "CreditRecharged":
                                history.add(new CreditRecharged(
                                        aggregateId,
                                        seq,
                                        p.getInteger("newCredit")
                                ));
                                break;

                            case "CreditUpdated":
                                history.add(new domain.events.CreditUpdated(
                                        aggregateId,
                                        seq,
                                        p.getInteger("newCredit")
                                ));
                                break;

                            default:
                                throw new IllegalStateException(
                                        "Unknown event type in store: " + type
                                );
                        }
                    }

                    fut.complete(history);
                })
                .onFailure(err -> fut.completeExceptionally(
                        new RuntimeException("Failed to load events: " + err.getMessage(), err)
                ));

        return fut;
    }
}
