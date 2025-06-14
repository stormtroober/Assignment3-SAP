package domain.events;

public interface UserEvent {
    String getAggregateId();       // es. username
    long   getSequence();          // version
    long   getOccurredAt();        // timestamp
    String getType();              // nome dell’evento
}
