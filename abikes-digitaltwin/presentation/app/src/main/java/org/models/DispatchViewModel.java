package org.models;

import io.vertx.core.json.JsonObject;
import java.awt.Color;

/**
 * A small view‐model for “bike dispatch” events—
 * carries the bikeId, target location, and an optional display color.
 */
public class DispatchViewModel {

    private final String bikeId;
    private final double x;
    private final double y;
    private final Color color;

    public DispatchViewModel(String bikeId, double x, double y) {
        this(bikeId, x, y, Color.RED);
    }

    public DispatchViewModel(String bikeId, double x, double y, Color color) {
        this.bikeId = bikeId;
        this.x      = x;
        this.y      = y;
        this.color  = color;
    }

    /** Build from the JSON payload your verticle is emitting. */
    public static DispatchViewModel fromJson(JsonObject json) {
        String bikeId = json.getString("bikeId");
        JsonObject loc = json.getJsonObject("location");
        double x = loc.getDouble("x");
        double y = loc.getDouble("y");
        return new DispatchViewModel(bikeId, x, y);
    }

    // ─── Getters ──────────────────────────────────────────────────────

    public String getBikeId() {
        return bikeId;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public Color getColor() {
        return color;
    }
}
