package domain.model;

public class Slot {
    private final String id;
    private String abikeId;

    public Slot(String id) {
        this.id = id;
        this.abikeId = null;
    }

    public String getId() { return id; }
    public String getAbikeId() { return abikeId; }
    public boolean isOccupied() { return abikeId != null; }

    public void occupy(String abikeId) { this.abikeId = abikeId; }
    public void free() { this.abikeId = null; }
}