package domain.model.repository;

import domain.model.P2d;

public interface DispatchRepository {

    void saveDispatchPosition(String userId, String bikeId, P2d position);

    P2d getDispatchPosition(String userId, String bikeId);

    void removeDispatchPosition(String userId, String bikeId);
}
