package org.models;

import java.awt.*;

public record BikeViewModel(
    String id,
    double x,
    double y,
    int batteryLevel,
    EBikeState state,
    BikeType type,
    Color color
) {
    public enum EBikeState { AVAILABLE, IN_USE, MAINTENANCE }
    public enum BikeType { NORMAL, AUTONOMOUS }

    private static final Color DEFAULT_COLOR = Color.BLACK;

    public BikeViewModel(String id, double x, double y, int batteryLevel, EBikeState state, BikeType type) {
        this(id, x, y, batteryLevel, state, type, DEFAULT_COLOR);
    }

    public BikeViewModel updateBatteryLevel(int batteryLevel) {
        return new BikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public BikeViewModel updateState(EBikeState state) {
        return new BikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public BikeViewModel updateColor(Color color) {
        return new BikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public BikeViewModel updateLocation(double newX, double newY) {
        return new BikeViewModel(id, newX, newY, batteryLevel, state, type, color);
    }
}