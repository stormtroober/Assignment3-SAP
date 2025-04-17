package domain.model;

import ddd.Factory;

public class EBikeFactory implements Factory {

    private static EBikeFactory instance;

    public static EBikeFactory getInstance() {
        if (instance == null) {
            instance = new EBikeFactory();
        }
        return instance;
    }

    private EBikeFactory() {
    }

    public EBike  createEBike(String id, float x, float y
                              ,EBikeState state
                              ,int battery
    ) {
        return new EBike(id, new P2d(x, y), state, battery);
    }

}
