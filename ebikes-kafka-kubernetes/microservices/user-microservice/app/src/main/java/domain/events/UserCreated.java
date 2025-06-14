package domain.events;

public final class UserCreated implements UserEvent {
    private final String aggregateId;
    private final long   occurredAt;
    private final long   sequence;
    private final String type = "UserCreated";

    // campi payload
    private final String userType;
    private final int    initialCredit;

    public UserCreated(String aggregateId,
                       long   sequence,
                       String userType,
                       int    initialCredit)
    {
        this.aggregateId   = aggregateId;
        this.sequence      = sequence;
        this.userType      = userType;
        this.initialCredit = initialCredit;
        this.occurredAt    = System.currentTimeMillis();
    }

    @Override public String getAggregateId()  { return aggregateId; }
    @Override public long   getSequence()     { return sequence; }
    @Override public long   getOccurredAt()   { return occurredAt; }
    @Override public String getType()         { return type; }

    public String getUserType()     { return userType; }
    public int    getInitialCredit(){ return initialCredit; }
}
