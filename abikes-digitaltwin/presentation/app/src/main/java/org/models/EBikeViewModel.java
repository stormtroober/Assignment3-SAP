package org.models;


public record EBikeViewModel(
    String id,
    double x,
    double y,
    int batteryLevel,
    EBikeState state,
    BikeType type
) {

    public EBikeViewModel(String id, double x, double y, int batteryLevel, EBikeState state, BikeType type) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.batteryLevel = batteryLevel;
        this.state = state;
        this.type = type;
    }
}