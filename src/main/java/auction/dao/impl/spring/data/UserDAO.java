package auction.dao.impl.spring.data;

import auction.model.User;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface UserDAO extends CrudRepository<User, Long> {
    Optional<User> findByLogin(String login);
}
