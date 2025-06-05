package me.viktrl.VpnTgBot.service;

import com.google.gson.GsonBuilder;
import jakarta.ws.rs.core.Link;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.viktrl.VpnTgBot.config.BotConfig;
import me.viktrl.VpnTgBot.model.Promocodes;
import me.viktrl.VpnTgBot.model.PromocodesRepository;
import me.viktrl.VpnTgBot.model.User;
import me.viktrl.VpnTgBot.model.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    UserRepository userRepository;
    @Autowired
    PromocodesRepository promocodesRepository;
    BotConfig config;
    static String apiUrl;
    static String admin;

    public TelegramBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;
        apiUrl = config.getOutlineApiUrl();
        admin = config.getAdmin();

        List<BotCommand> listCommand = new ArrayList<>();
        listCommand.add(new BotCommand("/start", "Начать"));
        // listCommand.add(new BotCommand("Зарегистрировать ключ", "Создать ВПН"));
        // listCommand.add(new BotCommand("Мои данные", "Мои данные"));
        // listCommand.add(new BotCommand("Мой ключ", "Мой ключ доступа"));
        // listCommand.add(new BotCommand("Инструкция", "Инструкция"));

        try {
            this.execute(new SetMyCommands(listCommand, new BotCommandScopeDefault(), "en"));
            scheduleDailyTask(14, 00);
        } catch (Exception e) {
            log.error("Ошибка при инициализации класса: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasCallbackQuery()) {
            String callBackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.startsWith("delete_")) {
                String username = callBackData.substring(7);
                deleteKeyByUsername(chatId, username);
            }
        } else if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (message) {
                case "/start":
                    registerUser(update.getMessage());
                    break;
                case "Зарегистрировать ключ":
                    registerKey(update.getMessage());
                    break;
                case "Мои данные":
                    showUserAccount(update.getMessage());
                    break;
                case "Мой ключ":
                    showUserKey(update.getMessage());
                    break;
                case "Инструкция":
                    sendMessage(chatId,
                            """
                                    1. Скопируйте ключ доступа (используйте команду "Мой ключ")
                                    
                                    2. Скачайте и установите подходящее вашему устройству приложение Outline:\s
                                    iOS: https://itunes.apple.com/app/outline-app/id1356177741
                                    Android: https://play.google.com/store/apps/details?id=org.outline.android.client
                                    macOS: https://itunes.apple.com/app/outline-app/id1356178125
                                    Windows: https://s3.amazonaws.com/outline-releases/client/windows/stable/Outline-Client.exe
                                    Linux: https://s3.amazonaws.com/outline-releases/client/linux/stable/Outline-Client.AppImage
                                    Дополнительная ссылка для Android: https://s3.amazonaws.com/outline-releases/client/android/stable/Outline-Client.apk
                                    
                                    3. Откройте клиент Outline. Если ваш ключ доступа определился автоматически, нажмите "Подключиться". Если этого не произошло, вставьте ключ в поле и нажмите "Подключиться".
                                    
                                    Теперь у вас есть доступ к свободному интернету. Чтобы убедиться, что вы подключились к серверу, зайдите на 2ip.ru, и проверьте IP.
                                    """);
                    break;
//                case "Оформить подписку":
//                    PaymentService paymentService = new PaymentService();
//                    sendMessage(chatId, paymentService.createPayment().getConfirmation().getConfirmationUrl());
//                    break;
                case "Изменить сервер":
                    sendMessage(chatId, "В разработке");
                    break;
                case "Админ панель":
                    adminPanelCommand(update.getMessage());
                    break;
                case "Удалить ключ":
                    showUsersListToKeyDelete(update.getMessage());
                    break;
                case "Обновить данные бота":
                    saveInDatabaseTrafficUsedByUser();
                    PromoCodeGenerator promoCodeGenerator = new PromoCodeGenerator(promocodesRepository);
                    for (Long userId : userRepository.listOfRegisteredUsers()) {
                        promoCodeGenerator.generateAndSaveUniqueCode(userId);
                    }
                    sendMessage(chatId, "Данные обновлены");
                    break;
                default:
                    sendMessage(chatId, "Этой команды не существует");
            }
        }
    }

    private void sendMessage(Long chatId, String textToSend) {
        SendMessage message = new SendMessage();

        User user = userRepository.findById(chatId).get();

        message.setChatId(chatId);
        message.setText(textToSend);

        ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
        replyKeyboardMarkup.setResizeKeyboard(true);
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();
        if (user.getToken() == null) {
            row.add("Зарегистрировать ключ");
        }
        if (user.getToken() != null) {
            row.add("Мои данные");
        }

        keyboardRows.add(row);

        row = new KeyboardRow();
        if (user.getToken() != null) {
            row.add("Мой ключ");
            row.add("Инструкция");
            keyboardRows.add(row);

            row = new KeyboardRow();
            row.add("Изменить сервер");
            keyboardRows.add(row);
//
//            row = new KeyboardRow();
//            row.add("Оформить подписку");
//            keyboardRows.add(row);
        }

        if (user.getUsername().equals(admin)) {
            row = new KeyboardRow();
            row.add("Админ панель");
            keyboardRows.add(row);
        }

        replyKeyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error yopta: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        User user = new User();
        var chatId = message.getChatId();

        if (userRepository.findById(message.getFrom().getId()).isEmpty()) {
            user.setChatId(chatId);
            if (message.getFrom().getFirstName() != null && message.getFrom().getLastName() != null && message.getFrom().getUserName() != null) {
                user.setFirstName(message.getFrom().getFirstName());
                user.setLastName(message.getFrom().getLastName());
                user.setUsername(message.getFrom().getUserName());
            }
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);

            PromoCodeGenerator promoCodeGenerator = new PromoCodeGenerator(promocodesRepository);
            promoCodeGenerator.generateAndSaveUniqueCode(user.getChatId());

//            log.info("User registered: " + user);

            Promocodes promoTable = new Promocodes();
            String AnswerUserSuccessCreated = "Добрый день! Вы успешно зарегистрировались.\nВаш логин в системе: "
                    + user.getUsername() + "\nВаш промокод: " + promoTable.getCode() + "\n\nСоздайте токен используя команду \"Зарегистрировать ключ\"";

            sendMessage(chatId, AnswerUserSuccessCreated);
        } else {
            if (message.getFrom().getFirstName() != null) {
                sendMessage(chatId, "Привет, " + message.getFrom().getFirstName());
            } else {
                sendMessage(chatId, "Привет!");
            }
        }
    }

    private void registerKey(Message message) {
        try {
            var chatId = message.getChatId();
            User user = userRepository.findById(chatId).get();
            if(userRepository.findById(message.getFrom().getId()).isPresent()) {
                if (user.getToken() == null) {
                    String newKeyId;

                    if (user.getUsername() != null) {
                        newKeyId = Requests.registerKey(user.getUsername()).getId();
                    } else {
                        newKeyId = Requests.registerKey(String.valueOf(user.getChatId())).getId();
                    }

                    user.setToken(Requests.getAccessKey(newKeyId).getId());
                    user.setTokenKey(Requests.getAccessKey(newKeyId).getAccessUrl());
                    user.setTokenCreatedAt(new Timestamp(System.currentTimeMillis()));
                    userRepository.save(user);

                    String answerToUser = "Бесплатный ВПН создан. Ключ:\n" + user.getTokenKey()
                            + "\n\nИспользуйте команду \"Инструкция\", чтобы получить инструкцию к легкой установке и настройке ВПН";

                    sendMessage(chatId, answerToUser);
                } else {
                    sendMessage(chatId, "У вас уже есть ключ");
                }
            } else {
                sendMessage(chatId, "Вы не зарегистрированы. Используйте команду /start");
            }
        } catch (Exception e) {
            log.error("Error yopta: " + e.getMessage());
        }
    }

    private void showUserAccount(Message message) {
        var chatId = message.getChatId();

        try {
            if (userRepository.findById(message.getFrom().getId()).isPresent()) {
                User user = userRepository.findById(message.getFrom().getId()).get();
                Long promocode = promocodesRepository.findPromocodeByUserId(chatId);
                Promocodes promocodes = promocodesRepository.findById(promocode).get();

                if (message.getChat().getUserName().equals(admin)) {
                    Map<String, Double> answerForMe = new LinkedHashMap<>();
                    userRepository.listOfActiveUsers().forEach(arr ->
                            answerForMe.put((String) arr[0], (Double) arr[1])
                    );

                    List<String> answerInactiveForMe = userRepository.listOfInactiveUsers();

                    String showAdminAnswer = "Логин: " + user.getUsername() +
                            "\nID: " + user.getToken() +
                            "\nКлюч для ВПН: " + user.getTokenKey() +
                            "\nПромокод: " + promocodes.getCode() +
                            "\nИспользовано трафика:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(answerForMe) +
                            "\nНе используют ключ:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(answerForMe) +
                            "\nНеактивированные пользователи:\n" + new GsonBuilder().setPrettyPrinting().create().toJson(answerInactiveForMe);

                    sendMessage(chatId, showAdminAnswer);
                } else {
                    Double trafficByUser = user.getTrafficUsed();
                    String showUserAnswer = "Логин: " + user.getUsername() +
                            "\nID: " + user.getToken() +
                            "\nКлюч для ВПН: " + user.getTokenKey() +
                            "\nИспользовано трафика: " + trafficByUser + " GB" +
                            "\nПромокод: " + promocodes.getCode();

                    sendMessage(chatId, showUserAnswer);
                }
            } else {
                sendMessage(chatId, "Вы не зарегистрированы. Используйте команду /start");
            }
        } catch (Exception e) {
            sendMessage(chatId, "Что то пошло не так. Обратитесь в поддержку");
        }
    }

    private void showUserKey(Message message) {
        var chatId = message.getChatId();

        try {
            User user = userRepository.findById(message.getFrom().getId()).get();
            if (userRepository.findById(message.getFrom().getId()).isPresent()) {
                if (user.getTokenKey() != null) {
                    sendMessage(chatId, user.getTokenKey());
                } else {
                    sendMessage(chatId, "У вас нет ключа");
                }
            } else {
                sendMessage(chatId, "Вы не зарегистрированы. Используйте команду /start");
            }
        } catch (Exception e) {
            sendMessage(chatId, "Что то пошло не так. Обратитесь в поддержку");
        }
    }

    private void adminPanelCommand(Message message) {
        var chatId = message.getChatId();
        User user = userRepository.findById(chatId).get();

        if (user.getUsername().equals(admin)) {
            ReplyKeyboardMarkup replyKeyboardMarkup = new ReplyKeyboardMarkup();
            replyKeyboardMarkup.setResizeKeyboard(true);
            List<KeyboardRow> keyboardRows = new ArrayList<>();

            KeyboardRow row = new KeyboardRow();
            row.add("Удалить ключ");
            keyboardRows.add(row);

            row = new KeyboardRow();
            row.add("Обновить данные бота");
            keyboardRows.add(row);

            replyKeyboardMarkup.setKeyboard(keyboardRows);

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Добро пожаловать в админ-панель!");
            sendMessage.setReplyMarkup(replyKeyboardMarkup);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Error yopta: " + e.getMessage());
            }
        }
    }

    private void showUsersListToKeyDelete(Message message) {
        var chatId = message.getChatId();
        User user = userRepository.findById(chatId).get();

        if (user.getUsername().equals(admin)) {
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();

            userRepository.findAll().forEach(el -> {
                if (el.getTokenKey() != null && el.getTrafficUsed() == null) {
                    InlineKeyboardButton buttonRow = new InlineKeyboardButton();
                    buttonRow.setText(el.getUsername());
                    buttonRow.setCallbackData("delete_" + el.getUsername()); // Уникальная callback data
                    rowsInline.add(Collections.singletonList(buttonRow));
                }
            });

            SendMessage sendMessage = new SendMessage();
            sendMessage.setChatId(chatId);
            sendMessage.setText("Выберите пользователя для удаления ключа:");
            inlineKeyboardMarkup.setKeyboard(rowsInline);
            sendMessage.setReplyMarkup(inlineKeyboardMarkup);

            try {
                execute(sendMessage);
            } catch (TelegramApiException e) {
                log.error("Error while sending inline keyboard: " + e.getMessage());
            }
        }
    }

    private void deleteKeyByUsername(Long chatId, String username) {
        Map<String, String> unActiveUsersMapByUsernameAndTokenId = new LinkedHashMap<>();

        userRepository.findAll().forEach(el -> {
            if (el.getTokenKey() != null && el.getTrafficUsed() == null) {
                unActiveUsersMapByUsernameAndTokenId.put(el.getUsername(), el.getToken());
            }
        });

        Map<String, Long> unActiveUsersMapByUsernameAndChatId = new LinkedHashMap<>();

        userRepository.findAll().forEach(el -> {
            if (el.getTokenKey() != null && el.getTrafficUsed() == null) {
                unActiveUsersMapByUsernameAndChatId.put(el.getUsername(), el.getChatId());
            }
        });

        Optional<User> optionalUser = userRepository.findById(unActiveUsersMapByUsernameAndChatId.get(username));

        try {
            if (optionalUser.isPresent()) {
                User user = optionalUser.get();

                Requests.deleteKey(unActiveUsersMapByUsernameAndTokenId.get(username));

                user.setToken(null);
                user.setTokenKey(null);
                userRepository.save(user);

                String answerToUser = "Ключ пользователя " + unActiveUsersMapByUsernameAndTokenId.get(username) + " удален";
                sendMessage(chatId, answerToUser);
            } else {
                sendMessage(chatId, "Пользователь не найден");
            }
        } catch (Exception e) {
            log.error("Error in deleteKeyByUsername method: " + e.getMessage());
        }
    }

    public void sendUserMessageAboutTrafficUsed() {
        try {
            Map<Long, Double> activeUsersMapByChatIdAndTrafficUsed = new LinkedHashMap<>();

            userRepository.findAll().forEach(el -> {
                if (el.getTrafficUsed() != null) {
                    activeUsersMapByChatIdAndTrafficUsed.put(el.getChatId(), el.getTrafficUsed());
                }
            });

            for (Long chatId : activeUsersMapByChatIdAndTrafficUsed.keySet()) {
                sendMessage(chatId, "Использовано трафика: " + activeUsersMapByChatIdAndTrafficUsed.get(chatId) + " GB");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveInDatabaseTrafficUsedByUser() {
        try {
            Map<String, Long> activeUsersMapByTokenIdAndChatId = new HashMap<>();

            userRepository.findAll().forEach(el -> {
                try {
                    if (Requests.getUsedTrafficByUser().containsKey(el.getToken())) {
                        activeUsersMapByTokenIdAndChatId.put(el.getToken(), el.getChatId());
                    }
                } catch (IOException e) {
                    System.out.println("Ошибка в saveInDatabaseTrafficUsedByUser(): " + e.getMessage());
                }
            });

            for (String activeToken : activeUsersMapByTokenIdAndChatId.keySet()) {
                User user = userRepository.findById(activeUsersMapByTokenIdAndChatId.get(activeToken)).get();
                Map<String, Double> getUsedTrafficByUserInGb = new LinkedHashMap<>();

                Requests.getUsedTrafficByUser().entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .forEachOrdered(e -> getUsedTrafficByUserInGb.put(e.getKey(),
                                Math.round((e.getValue() / 1_073_741_824.0) * 100.0) / 100.0));

                user.setTrafficUsed(getUsedTrafficByUserInGb.get(activeToken));
                userRepository.save(user);
            }
        } catch (Exception e) {
            System.out.println(
                    "Ошибка при сохранении в БД трафка пользователей: " + e.getMessage() + "\n" + e.getStackTrace());
        }
    }

    private void scheduleDailyTask(int targetHour, int targetMinute) {
        try {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            Runnable task = () -> {
                System.out.println("Запуск задачи: " + LocalDateTime.now());
                saveInDatabaseTrafficUsedByUser();
                sendUserMessageAboutTrafficUsed();
            };

            long initialDelay = calculateInitialDelay(targetHour, targetMinute);
            long period = 24 * 60 * 60; // 24 часа в секундах

            scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.out.println("Ошибка при выполннии задачи по расаписанию" + e.getMessage());
        }
    }

    private long calculateInitialDelay(int targetHour, int targetMinute) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        ZonedDateTime nextRun = now.withHour(targetHour).withMinute(targetMinute).withSecond(0);

        if (now.compareTo(nextRun) > 0) {
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();
    }
}
