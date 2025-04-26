package domain.model;

import ddd.Aggregate;

public class Station implements Aggregate<String> {
  private final String id;
  private final P2d position;

  public Station(String id, P2d position) {
    this.id = id;
    this.position = position;
  }

  @Override
  public String getId() {
    return id;
  }

  public P2d getPosition() {
    return position;
  }

  @Override
  public String toString() {
    return String.format("Station{id='%s', position=%s}", id, position);
  }
}
