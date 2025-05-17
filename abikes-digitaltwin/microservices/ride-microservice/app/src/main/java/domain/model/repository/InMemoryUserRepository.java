package domain.model.repository;

import domain.model.User;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InMemoryUserRepository implements UserRepository {
  private static final Logger logger = LoggerFactory.getLogger(InMemoryUserRepository.class);
  private final Map<String, User> users = new ConcurrentHashMap<>();

  @Override
  public void save(User user) {
    users.put(user.getId(), user);
    logger.info("User saved/updated: {}", user);
  }

  @Override
  public Optional<User> findById(String username) {
    return Optional.ofNullable(users.get(username));
  }

  @Override
  public Map<String, User> findAll() {
    return Map.copyOf(users);
  }
}
