package domain.model;

import ddd.Aggregate;


import java.io.Serializable;

public class EBike implements Aggregate<String>, Serializable {

    private final String bikeName;
    private P2d position;
    private EBikeState state;
    private int batteryLevel;

    public EBike(String bikeName, P2d position, EBikeState state, int batteryLevel) {
        this.bikeName = bikeName;
        this.position = position;
        this.state = state;
        this.batteryLevel = batteryLevel;
    }

    public String getBikeName() {
        return bikeName;
    }

    public P2d getPosition() {
        return position;
    }

    public EBikeState getState() {
        return state;
    }

    public int getBatteryLevel() {
        return batteryLevel;
    }

    @Override
    public String toString() {
        return "EBike{" +
                "bikeName='" + bikeName + '\'' +
                ", position=" + position +
                ", state=" + state +
                ", batteryLevel=" + batteryLevel +
                '}';
    }

    @Override
    public String getId() {
        return bikeName;
    }
}