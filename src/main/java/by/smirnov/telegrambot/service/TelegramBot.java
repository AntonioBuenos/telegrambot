package by.smirnov.telegrambot.service;

import by.smirnov.telegrambot.config.BotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@Slf4j
@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;

    @Override
    public String getBotUsername() {
        return botConfig.getBotName();
    }

    @Override
    public String getBotToken() {
        return botConfig.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            log.info(update.getMessage().getChat().getFirstName() + " sent message: " + messageText);
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    break;
                default:
                    sendMessage(chatId, "Все говорят \"" + update.getMessage().getText() + "\", а ты возьми и приди! еще и подарок принеси!))");
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = "Привет, " + name + ", я бот! Меня создал Антон, потому что ему лень самому общаться в телеграме.\n" +
                "А теперь к делу: приходи к нему на ДР! Что скажешь?";
        log.info("Replied to user " + name);

        sendMessage(chatId, answer);

    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }
}
