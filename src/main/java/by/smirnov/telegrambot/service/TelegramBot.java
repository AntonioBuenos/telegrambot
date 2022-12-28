package by.smirnov.telegrambot.service;

import by.smirnov.telegrambot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private List<BotCommand> listofCommands;
    private static final String HELP_TEXT = """
            This bot is created to demonstrate Spring capabilities.
            You can execute commands from the main menu on the left or by typing a command:
            Type /start to see a welcome message
            Type /mydata to see data stored about yourself
            Type /help to see this message again""";
    private static final String DEFAULT_TEXT = "Все говорят \"%s\", а ты возьми и приди! еще и подарок принеси!))";
    private static final String START_TEXT = """
            Привет, %s, я бот! Меня создал Антон, потому что ему лень самому общаться в телеграме.
            А теперь к делу: приходи к нему на ДР! Что скажешь?)""";
    public TelegramBot(BotConfig botConfig) {
        this.botConfig = botConfig;
        initListOfCommands();
    }

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
                case "/start" -> startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                default -> sendMessage(chatId, String.format(DEFAULT_TEXT, update.getMessage().getText()));
            }
        }
    }

    private void startCommandReceived(long chatId, String name) {
        String answer = String.format(START_TEXT, name);
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

    private void initListOfCommands(){
        listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "info how to use this bot"));
        listofCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }
}
