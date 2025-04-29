package org.models;

import java.awt.*;

public record EBikeViewModel(
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

    public EBikeViewModel(String id, double x, double y, int batteryLevel, EBikeState state, BikeType type) {
        this(id, x, y, batteryLevel, state, type, DEFAULT_COLOR);
    }

    public EBikeViewModel updateBatteryLevel(int batteryLevel) {
        return new EBikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public EBikeViewModel updateState(EBikeState state) {
        return new EBikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public EBikeViewModel updateColor(Color color) {
        return new EBikeViewModel(id, x, y, batteryLevel, state, type, color);
    }

    public EBikeViewModel updateLocation(double newX, double newY) {
        return new EBikeViewModel(id, newX, newY, batteryLevel, state, type, color);
    }
}