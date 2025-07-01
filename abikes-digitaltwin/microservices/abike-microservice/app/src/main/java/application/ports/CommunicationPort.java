package application.ports;

import java.util.List;

/**
 * Generic communication port for sending updates about entities.
 *
 * @param <T> the type of entity to be communicated
 */
public interface CommunicationPort<T> {

  /**
   * Sends an update for a single entity.
   *
   * @param entity the entity to send an update for
   */
  void sendUpdate(T entity);

  /**
   * Sends updates for a list of entities.
   *
   * @param entities the list of entities to send updates for
   */
  void sendAllUpdates(List<T> entities);
}
