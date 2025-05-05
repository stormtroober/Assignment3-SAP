package domain.model;

import java.util.ArrayList;
import java.util.List;


public class StationFactory {

    public static final int MAX_SLOTS = 4;

    public static Station createStandardStation(String id) {
        List<Slot> slots = new ArrayList<>();
        for (int i = 0; i < MAX_SLOTS; i++) {
            slots.add(new Slot(id + "-slot" + i));
        }
        return new Station(id, new P2d(15, 15), slots);
    }
}