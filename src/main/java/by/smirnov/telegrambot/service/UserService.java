package by.smirnov.telegrambot.service;

import by.smirnov.telegrambot.model.User;
import org.telegram.telegrambots.meta.api.objects.Message;

import java.util.List;

public interface UserService {

    void registerUser(Message message);
    List<User> findAll();
    User findById(long id);
}
