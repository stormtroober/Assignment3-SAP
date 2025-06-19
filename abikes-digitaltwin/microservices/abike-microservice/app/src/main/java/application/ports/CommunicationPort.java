package application.ports;

import java.util.List;

public interface CommunicationPort<T> {
    void sendUpdate(T entity);
    void sendAllUpdates(List<T> entities);
}