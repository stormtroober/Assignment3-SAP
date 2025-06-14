package domain.events;

public final class CreditRecharged implements UserEvent {
    private final String aggregateId;
    private final long   occurredAt;
    private final long   sequence;
    private final String type = "CreditRecharged";

    // payload
    private final int amount;

    public CreditRecharged(String aggregateId, long sequence, int amount) {
        this.aggregateId = aggregateId;
        this.sequence    = sequence;
        this.amount      = amount;
        this.occurredAt  = System.currentTimeMillis();
    }

    @Override public String getAggregateId()  { return aggregateId; }
    @Override public long   getSequence()     { return sequence; }
    @Override public long   getOccurredAt()   { return occurredAt; }
    @Override public String getType()         { return type; }

    public int getAmount() { return amount; }
}
