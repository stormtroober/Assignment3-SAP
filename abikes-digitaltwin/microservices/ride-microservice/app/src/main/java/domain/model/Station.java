package domain.model;

import ddd.Aggregate;

import java.util.List;

public class Station implements Aggregate<String> {
  private final String id;
  private final P2d location;
  private final List<Slot> slots;
  private final int maxSlots;

  public Station(String id, P2d location, List<Slot> slots, int maxSlots) {
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

  public List<Slot> getSlots() {
    return slots;
  }

  public int getMaxSlots() {
    return maxSlots;
  }
}
