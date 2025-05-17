package domain.model.repository;

import domain.model.User;
import java.util.Map;
import java.util.Optional;

public interface UserRepository {
    void save(User user);

    Optional<User> findById(String username);

    Map<String, User> findAll();
}
