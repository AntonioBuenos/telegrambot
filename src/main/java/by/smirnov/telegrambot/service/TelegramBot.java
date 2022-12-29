package by.smirnov.telegrambot.service;

import by.smirnov.telegrambot.config.BotConfig;
import by.smirnov.telegrambot.model.User;
import by.smirnov.telegrambot.repository.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserRepository userRepository;
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
            А теперь к делу: приходи к нему на ДР! Что скажешь? %s""";
    private static final String SMILE_BLUSH = ":blush:";
    private static final String YES_BUTTON = "YES_BUTTON";
    private static final String NO_BUTTON = "NO_BUTTON";
    private static final String ERROR_TEXT = "Error occurred: ";

    public TelegramBot(BotConfig botConfig, UserRepository userRepository) {
        this.botConfig = botConfig;
        this.userRepository = userRepository;
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
                case "/start" -> {
                    registerUser(update.getMessage());
                    startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                }
                case "/help" -> sendMessage(chatId, HELP_TEXT);
                case "/register" -> register(chatId);
                default -> sendMessage(chatId, String.format(DEFAULT_TEXT, update.getMessage().getText()));
            }
        } else if (update.hasCallbackQuery()) {
            //для случаев, когда в апдейте имеется объект CallbackQuery (от кнопок илайн-клавиатуры)
            String callbackData = update.getCallbackQuery().getData(); //получаем данные из CallbackQuery
            Message message = update.getCallbackQuery().getMessage(); //самого сообщения в апдейте нет, оно внутри CallbackQuery
            long messageId = message.getMessageId(); //получаем ID сообщения
            long chatId = message.getChatId(); //получаем ID чата

            if (callbackData.equals(YES_BUTTON)) { //проверяем соответствие конкретных данных, полученных отнажатия кнопки
                String text = "You pressed YES button"; //новый текст, который заменит прежний текст сообщения
                executeEditMessageText(text, chatId, messageId); //метод изменения сообщения: новый текст заменит старый
            } else if (callbackData.equals(NO_BUTTON)) {
                String text = "You pressed NO button";
                executeEditMessageText(text, chatId, messageId);
            }
        }
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage(); //создаем объект отправки сообщений
        message.setChatId(String.valueOf(chatId)); //определяем ID чата
        message.setText("Do you really want to register?"); //и сообщение к отправке

        InlineKeyboardMarkup keybdMarkup = new InlineKeyboardMarkup(); //создаем объект клавиатуры, принадлежащий сообщению
        List<List<InlineKeyboardButton>> keybd = new ArrayList<>(); //аргумент клавиатуры требует списка списков кнопок
        List<InlineKeyboardButton> buttonsRow = new ArrayList<>(); //создаем список для кнопок (ряд кнопок)

        var yesButton = new InlineKeyboardButton(); //создаем кнопку
        yesButton.setText("Yes"); //надпись на кнопке
        yesButton.setCallbackData(YES_BUTTON); //значение, которое будет возвращать кнопка при нажатии

        var noButton = new InlineKeyboardButton();
        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        buttonsRow.add(yesButton); //добавляем кнопки в ряд (список кнопок)
        buttonsRow.add(noButton);

        keybd.add(buttonsRow); //добавляем ряд кнопок в клавиатуру

        keybdMarkup.setKeyboard(keybd); //передаем сформированную клавиатуру в объект клавиатуры сообщения
        message.setReplyMarkup(keybdMarkup); //устанавливаем сообщению объект его клавиатуры

        executeMessage(message); //в отдельном методе исполняется отправка сообщения с обработкой исключений
    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();
            User user = new User();
            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String name) {
        //Способ вставки эмодзи через библиотеку emoji-java.
        // Shortcodes можно смотреть, например, здесь: https://emojipedia.org/
        String answer = EmojiParser.parseToUnicode(String.format(START_TEXT, name, SMILE_BLUSH));

        log.info("Replied to user " + name);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);

        message.setReplyMarkup(getReplyKeys()); //передаем объект клавиатуры в сообщение

        try {
            execute(message); //отправляем сообщение
        } catch (TelegramApiException e) {
            log.error("Error occured: " + e.getMessage());
        }
    }

    private void initListOfCommands() {
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

    private ReplyKeyboardMarkup getReplyKeys() {

        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup(); //Объект класса клавиатуры вариантов ответов
        List<KeyboardRow> keyboardRows = new ArrayList<>(); //создаем список рядов кнопок

        KeyboardRow row = new KeyboardRow(); //создаем ряд с кнопками
        row.add("weather"); //заполняем ряд конкретными кнопками, передаваемое значение - надпись на кнопке + ответ,
        row.add("get random joke"); //который будет выдан при нажатии
        keyboardRows.add(row); //добавляем ряд в клавиатуру

        row = new KeyboardRow(); //создаем еще один ряд
        row.add("register"); //добавляем еще кнопки
        row.add("check my data");
        row.add("delete my data");
        keyboardRows.add(row); //добавляем ряд в клавиатуру

        keyboardMarkup.setKeyboard(keyboardRows); //список с рядами передаем в объект клавиатуры

        return keyboardMarkup;
    }

    private void executeEditMessageText(String text, long chatId, long messageId) {
        EditMessageText message = new EditMessageText(); //объект измененного текста сообщения
        message.setChatId(String.valueOf(chatId)); //устанавливаем сообщению ID чата
        message.setText(text); //и новый текст
        message.setMessageId((int) messageId); //и ID сообщения

        try {
            execute(message); //исполнение изменения сообщения
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR_TEXT + e.getMessage());
        }
    }
}
