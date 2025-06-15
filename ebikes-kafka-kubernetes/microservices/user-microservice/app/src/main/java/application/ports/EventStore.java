package application.ports;

import domain.events.UserEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public interface EventStore {
    /**
     * Appendi un evento. expectedVersion è la version dell’ultimo evento già applicato
     * (ottimistic lock).
     */
    CompletableFuture<Void> appendEvent(
            String aggregateId,
            UserEvent event,
            long expectedVersion
    );

    /**
     * Carica tutti gli eventi per questo aggregateId, a partire da fromSequence (inclusivo).
     */
    CompletableFuture<List<UserEvent>> loadEvents(
            String aggregateId,
            long fromSequence
    );

    CompletableFuture<List<UserEvent>> loadAllEvents();
}
