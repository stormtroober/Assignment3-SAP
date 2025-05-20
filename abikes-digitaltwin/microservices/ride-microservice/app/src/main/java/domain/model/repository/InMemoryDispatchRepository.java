package domain.model.repository;

import domain.model.P2d;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryDispatchRepository implements DispatchRepository {
    private final Map<String, P2d> dispatchPositions = new ConcurrentHashMap<>();

    public void saveDispatchPosition(String userId, String bikeId, P2d position) {
        String key = createKey(userId, bikeId);
        dispatchPositions.put(key, position);
    }

    public P2d getDispatchPosition(String userId, String bikeId) {
        String key = createKey(userId, bikeId);
        return dispatchPositions.get(key);
    }

    public void removeDispatchPosition(String userId, String bikeId) {
        String key = createKey(userId, bikeId);
        dispatchPositions.remove(key);
    }

    private String createKey(String userId, String bikeId) {
        return userId + "-" + bikeId;
    }
}