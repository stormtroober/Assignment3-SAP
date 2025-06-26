package application;

import application.ports.UserEventPublisher;
import domain.model.User;
import io.vertx.core.Vertx;

public class UserEventPublisherImpl implements UserEventPublisher {
  private final Vertx vertx;

  public UserEventPublisherImpl(Vertx vertx) {
    this.vertx = vertx;
  }

  @Override
  public void publishUserUpdate(String username, User user) {
    vertx.eventBus().publish(username, user.toJson());
  }

  @Override
  public void publishAllUsersUpdates(User user) {
    vertx.eventBus().publish("users.update", user.toJson());
  }
}
