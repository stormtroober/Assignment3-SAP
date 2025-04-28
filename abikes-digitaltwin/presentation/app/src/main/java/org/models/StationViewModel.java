        package org.models;

        import java.util.List;

        public class StationViewModel {
            private final String id;
            private final double x;
            private final double y;
            private final List<String> slots;
            private final int maxSlots;

            public StationViewModel(String id, double x, double y, List<String> slots, int maxSlots) {
                this.id = id;
                this.x = x;
                this.y = y;
                this.slots = slots;
                this.maxSlots = maxSlots;
            }

            public String getId() { return id; }
            public double getX() { return x; }
            public double getY() { return y; }
            public List<String> getSlots() { return slots; }
            public int getMaxSlots() { return maxSlots; }
        }