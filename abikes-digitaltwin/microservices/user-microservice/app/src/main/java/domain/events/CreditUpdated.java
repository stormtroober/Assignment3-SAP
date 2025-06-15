package domain.events;

public final class CreditUpdated implements UserEvent {
  private final String aggregateId;
  private final long occurredAt;
  private final long sequence;
  private final UserEventType type = UserEventType.CREDIT_UPDATED;

  // payload: nuovo valore di credito
  private final int newCredit;

  public CreditUpdated(String aggregateId, long sequence, int newCredit) {
    this.aggregateId = aggregateId;
    this.sequence = sequence;
    this.newCredit = newCredit;
    this.occurredAt = System.currentTimeMillis();
  }

  @Override
  public String getAggregateId() {
    return aggregateId;
  }

  @Override
  public long getSequence() {
    return sequence;
  }

  @Override
  public long getOccurredAt() {
    return occurredAt;
  }

  @Override
  public UserEventType getType() {
    return type;
  }

  /** Ritorna il credito dopo l’aggiornamento */
  public int getNewCredit() {
    return newCredit;
  }
}
