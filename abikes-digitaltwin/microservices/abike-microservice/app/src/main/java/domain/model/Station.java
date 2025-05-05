package domain.model;

import ddd.Aggregate;
import java.util.List;
import java.util.Optional;

public class Station implements Aggregate<String> {
    private final String id;
    private final P2d position;
    private final List<Slot> slots;

    public Station(String id, P2d position, List<Slot> slots) {
        this.id = id;
        this.position = position;
        this.slots = slots;
    }

    @Override
    public String getId() { return id; }
    public P2d getPosition() { return position; }
    public List<Slot> getSlots() { return slots; }

    public Optional<Slot> occupyFreeSlot(String abikeId) {
        for (Slot slot : slots) {
            if (!slot.isOccupied()) {
                slot.occupy(abikeId);
                return Optional.of(slot);
            }
        }
        return Optional.empty();
    }

    public boolean freeSlotByAbike(String abikeId) {
        for (Slot slot : slots) {
            if (abikeId.equals(slot.getAbikeId())) {
                slot.free();
                return true;
            }
        }
        return false;
    }
}