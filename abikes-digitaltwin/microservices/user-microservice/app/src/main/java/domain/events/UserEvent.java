package domain.events;

public interface UserEvent {
    String getAggregateId(); // es. username

    long getSequence(); // version

    long getOccurredAt(); // timestamp

    UserEventType getType(); // tipo dell'evento
}