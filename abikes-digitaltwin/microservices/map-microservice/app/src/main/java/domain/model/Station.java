package domain.model;

import ddd.Aggregate;
import java.util.List;

public class Station implements Aggregate<String> {
  private final String id;
  private final P2d location;
  private final List<String> slots; // List of bike IDs or slot identifiers
  private final int maxSlots;

  public Station(String id, P2d location, List<String> slots, int maxSlots) {
    this.id = id;
    this.location = location;
    this.slots = slots;
    this.maxSlots = maxSlots;
  }

  @Override
  public String getId() {
    return id;
  }

  public P2d getLocation() {
    return location;
  }

  public List<String> getSlots() {
    return slots;
  }

  public int getMaxSlots() {
    return maxSlots;
  }

  @Override
  public String toString() {
    return String.format("Station{id='%s', location=%s, slots=%s, maxSlots=%d}", id, location, slots, maxSlots);
  }
}