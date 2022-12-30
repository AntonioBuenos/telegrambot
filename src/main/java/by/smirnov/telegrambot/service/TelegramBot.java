package by.smirnov.telegrambot.service;

import by.smirnov.telegrambot.config.BotConfig;
import by.smirnov.telegrambot.model.Ads;
import by.smirnov.telegrambot.model.User;
import by.smirnov.telegrambot.repository.AdsRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
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

import java.util.ArrayList;
import java.util.List;

import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_DELETE_DATA;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_HELP;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_MY_DATA;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_REGISTER;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_SEND;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_SETTINGS;
import static by.smirnov.telegrambot.constants.BotConstants.COMMAND_START;
import static by.smirnov.telegrambot.constants.BotConstants.DEFAULT_TEXT;
import static by.smirnov.telegrambot.constants.BotConstants.ERROR;
import static by.smirnov.telegrambot.constants.BotConstants.ERROR_COMMAND_LIST;
import static by.smirnov.telegrambot.constants.BotConstants.HELP_TEXT;
import static by.smirnov.telegrambot.constants.BotConstants.LOG_REPLIED;
import static by.smirnov.telegrambot.constants.BotConstants.NO_BUTTON;
import static by.smirnov.telegrambot.constants.BotConstants.SMILE_BLUSH;
import static by.smirnov.telegrambot.constants.BotConstants.START_TEXT;
import static by.smirnov.telegrambot.constants.BotConstants.YES_BUTTON;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private final BotConfig botConfig;
    private final UserService userService;
    private final AdsRepository adsRepository;
    private List<BotCommand> listofCommands;

    public TelegramBot(BotConfig botConfig, UserService userService, AdsRepository adsRepository) {
        this.botConfig = botConfig;
        this.userService = userService;
        this.adsRepository = adsRepository;
        initListOfCommands(); //инициализируем список команд
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
            handleMessage(update);
        } else if (update.hasCallbackQuery()) {
            handleCallBackQuery(update); //когда в апдейте - ответ от кнопок инлайн-клавиатуры
        }
    }

    private void handleMessage(Update update){
        Message message = update.getMessage();
        String messageText = message.getText();
        log.info("{} sent message: {}", message.getChat().getUserName(), messageText);
        long chatId = message.getChatId();

        if (messageText.contains(COMMAND_SEND) && botConfig.getOwnerId() == chatId) {
            //проверяем ключевое слово и владельца бота. Владелец отправит сообщение боту, а бот разошлет всем юзерам
            String textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
            //парсим: отделяем сообщение от ключевого слова
            for (User user : userService.findAll()) { //проходимся по списку всех пользователей из БД
                sendMessage(user.getChatId(), textToSend); //каждому отправляем сообщение рассылки
            }
        } else {
            switch (messageText) {
                case COMMAND_START -> startCommandReceived(chatId, message);
                case COMMAND_HELP -> sendMessage(chatId, HELP_TEXT);
                case COMMAND_REGISTER -> register(chatId);
                default -> sendMessage(chatId, String.format(DEFAULT_TEXT, messageText));
            }
        }
    }

    private void handleCallBackQuery(Update update){
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

    private void startCommandReceived(long chatId, Message message) {
        userService.registerUser(message);
        String name = message.getChat().getFirstName();
        //Способ вставки эмодзи через библиотеку emoji-java.
        // Shortcodes можно смотреть, например, здесь: https://emojipedia.org/
        String answer = EmojiParser.parseToUnicode(String.format(START_TEXT, name, SMILE_BLUSH));

        log.info(LOG_REPLIED, message.getChat().getUserName());
        sendMsgWithReplyKbd(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        executeMessage(message);
    }

    private void sendMsgWithReplyKbd(long chatId, String textToSend) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(textToSend);
        message.setReplyMarkup(getReplyKeys()); //передаем объект клавиатуры в сообщение

        executeMessage(message);
    }

    private void initListOfCommands() { //инициализируем список команд
        listofCommands = new ArrayList<>(); //создаем список и добавляем новые команды
        listofCommands.add(new BotCommand(COMMAND_START, "get a welcome message"));
        listofCommands.add(new BotCommand(COMMAND_MY_DATA, "get your data stored"));
        listofCommands.add(new BotCommand(COMMAND_REGISTER, "registration"));
        listofCommands.add(new BotCommand(COMMAND_DELETE_DATA, "delete my data"));
        listofCommands.add(new BotCommand(COMMAND_HELP, "info how to use this bot"));
        listofCommands.add(new BotCommand(COMMAND_SETTINGS, "set your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
            //выполняется добавление списка команд
        } catch (TelegramApiException e) {
            log.error(ERROR_COMMAND_LIST, e.getMessage());
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
            log.error(ERROR, e.getMessage());
        }
    }

    private void executeMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error(ERROR, e.getMessage());
        }
    }

    @Scheduled(cron = "${cron.scheduler}") //определяем, что будет автоматическое выполнение метода по расписанию
    // + определяем метод
    private void sendAds() {

        for (Ads ad : adsRepository.findAll()) { //каждое объявление из БД
            for (User user : userService.findAll()) { //каждому пользователю
                sendMessage(user.getChatId(), ad.getAd()); //отправляем
            }
        }

    }
}
