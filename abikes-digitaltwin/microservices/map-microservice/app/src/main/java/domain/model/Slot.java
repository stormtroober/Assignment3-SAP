package domain.model;

import java.io.Serializable;

public class Slot implements Serializable {
  private final String id;
  private final String abikeId; // null if free

  public Slot(String id, String abikeId) {
    this.id = id;
    this.abikeId = abikeId;
  }

  public String getId() {
    return id;
  }

  public String getAbikeId() {
    return abikeId;
  }

  public boolean isOccupied() {
    return abikeId != null;
  }
}
