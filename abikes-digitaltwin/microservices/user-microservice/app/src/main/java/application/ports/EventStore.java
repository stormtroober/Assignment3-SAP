package application.ports;

import domain.events.UserEvent;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Event Store interface for managing user events in an event-sourced architecture. This interface
 * provides methods for appending and retrieving user events.
 */
public interface EventStore {
  /**
   * Appends an event to the event store for the specified aggregate. Uses optimistic locking
   * through the expectedVersion parameter.
   *
   * @param aggregateId The unique identifier for the aggregate
   * @param event The user event to append
   * @param expectedVersion The expected current version of the aggregate (for optimistic locking)
   * @return A CompletableFuture that completes when the event has been appended
   */
  CompletableFuture<Void> appendEvent(String aggregateId, UserEvent event, long expectedVersion);

  /**
   * Loads all events for a specific aggregate, starting from the specified sequence number.
   *
   * @param aggregateId The unique identifier for the aggregate
   * @param fromSequence The sequence number to start loading events from (inclusive)
   * @return A CompletableFuture containing a list of user events for the specified aggregate
   */
  CompletableFuture<List<UserEvent>> loadEvents(String aggregateId, long fromSequence);

  /**
   * Loads all user events from the event store across all aggregates.
   *
   * @return A CompletableFuture containing a list of all user events
   */
  CompletableFuture<List<UserEvent>> loadAllEvents();
}
