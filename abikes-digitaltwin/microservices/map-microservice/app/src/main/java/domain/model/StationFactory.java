package domain.model;

import java.util.Collections;
import java.util.List;

public class StationFactory {
    private static final StationFactory INSTANCE = new StationFactory();

    private StationFactory() {}

    public static StationFactory getInstance() {
        return INSTANCE;
    }

    public Station createStation(String id, float x, float y, List<Slot> slots, int maxSlots) {
        return new Station(
            id, new P2d(x, y), slots != null ? slots : Collections.emptyList(), maxSlots);
    }
}