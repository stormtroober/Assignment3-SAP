package domain.model;

import ddd.Aggregate;
import java.util.List;

public class Station implements Aggregate<String> {
  private final String id;
  private final P2d position;
  private final List<String> slots; // List of bike IDs or slot identifiers

  public Station(String id, P2d position, List<String> slots) {
    this.id = id;
    this.position = position;
    this.slots = slots;
  }

  @Override
  public String getId() {
    return id;
  }

  public P2d getPosition() {
    return position;
  }

  public List<String> getSlots() {
    return slots;
  }

  @Override
  public String toString() {
    return String.format("Station{id='%s', position=%s, slots=%s}", id, position, slots);
  }
}
