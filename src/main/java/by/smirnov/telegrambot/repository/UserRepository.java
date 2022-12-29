package by.smirnov.telegrambot.repository;

import by.smirnov.telegrambot.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, Long> {
}
