package me.viktrl.VpnTgBot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.dynomake.outline.OutlineWrapper;
import me.viktrl.VpnTgBot.config.BotConfig;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import javax.net.ssl.*;
import java.io.*;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
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
    BotConfig config;
    String apiUrl;
    OutlineWrapper outlineWrapper;

    public TelegramBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;
        this.apiUrl = config.getOutlineApiUrl();
        this.outlineWrapper = OutlineWrapper.create(apiUrl);

        List<BotCommand> listCommand = new ArrayList<>();
        listCommand.add(new BotCommand("/start", "Начать"));
//        listCommand.add(new BotCommand("Зарегистрировать ключ", "Создать ВПН"));
//        listCommand.add(new BotCommand("Мои данные", "Мои данные"));
//        listCommand.add(new BotCommand("Мой ключ", "Мой ключ доступа"));
//        listCommand.add(new BotCommand("Инструкция", "Инструкция"));

        try {
            this.execute(new SetMyCommands(listCommand, new BotCommandScopeDefault(), "en"));
            scheduleDailyTask(13, 13);
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
        if (update.hasMessage() && update.getMessage().hasText()) {
            String message = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (message) {
                case "/start":
                    startCommand(chatId, "Привет, " + update.getMessage().getChat().getFirstName());
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
                    startCommand(chatId, """
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
                default:
                    startCommand(chatId, "Этой команды не существует");
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
        }

        replyKeyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(replyKeyboardMarkup);

        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error yopta: " + e.getMessage());
        }
    }

    private void startCommand(Long chatId, String name) {
        try {
            sendMessage(chatId, name);
        } catch (Exception e) {
            System.out.println("3Error yopta: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        User user = new User();
        var chatId = message.getChatId();

        if (userRepository.findById(message.getFrom().getId()).isEmpty()) {
            user.setChatId(chatId);
            user.setFirstName(message.getFrom().getFirstName());
            user.setLastName(message.getFrom().getLastName());
            user.setUsername(message.getFrom().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User registered: " + user);

            String AnswerUserSuccessCreated = "Добрый день! Вы успешно зарегистрировались.\nВаш логин в системе: " + user.getUsername() + "\n\nСоздайте токен используя команду \"Зарегистрировать ключ\"";
            try {
                execute(new SendMessage(String.valueOf(chatId), AnswerUserSuccessCreated));
            } catch (TelegramApiException e) {
                log.error("Error yopta: " + e.getMessage());
            }
        }
    }

    private void registerKey(Message message) {
        var chatId = message.getChatId();

        try {
            User user = userRepository.findById(message.getFrom().getId()).get();

            if (user.getToken() == null) {
                int newKeyId = outlineWrapper.generateKey().id;

                user.setToken(String.valueOf(outlineWrapper.getKey(newKeyId).id));
                user.setTokenKey(outlineWrapper.getKey(newKeyId).accessUrl);
                userRepository.save(user);

                String AnswerVpnSuccessCreated = "Бесплатный ВПН создан. Ключ:\n" + user.getTokenKey() + "\n\nИспользуйте команду \"Инструкция\", чтобы получить инструкцию к легкой установке и настройке ВПН";
                try {
                    execute(new SendMessage(String.valueOf(chatId), AnswerVpnSuccessCreated));
                } catch (TelegramApiException e) {
                    log.error("Error yopta: " + e.getMessage());
                }
            } else {
                try {
                    execute(new SendMessage(String.valueOf(chatId), "У вас уже есть ключ"));
                } catch (TelegramApiException e) {
                    log.error("Error yopta: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            try {
                execute(new SendMessage(String.valueOf(chatId), "Ошибка"));
            } catch (TelegramApiException e2) {
                log.error("Error yopta: " + e2.getMessage());
            }
        }
    }

    private void showUserAccount(Message message) {
        var chatId = message.getChatId();

        try {
            User user = userRepository.findById(message.getFrom().getId()).get();
            if (userRepository.findById(message.getFrom().getId()).isPresent()) {
                try {
                    if (message.getChat().getUserName().equals("unmaskked")) {
                        Map<String, Double> answerForMe = new LinkedHashMap<>();

                        userRepository.findAll().forEach(el -> answerForMe.put(el.getUsername(), el.getTrafficUsed()));

                        String prettyJsonAnswerForMe = new GsonBuilder().setPrettyPrinting().create().toJson(answerForMe);
                        String showAdminAnswer = "Логин: " + user.getUsername() + "\n" +
                                "ID: " + user.getToken() + "\n" +
                                "Ключ для ВПН: " + user.getTokenKey() + "\n" +
                                "Использовано трафика:\n" + prettyJsonAnswerForMe;
                        execute(new SendMessage(String.valueOf(chatId), showAdminAnswer));
                    } else {
                        Double trafficByUser = user.getTrafficUsed();
                        String showUserAnswer = "Логин: " + user.getUsername() + "\n" +
                                "ID: " + user.getToken() + "\n" +
                                "Ключ для ВПН: " + user.getTokenKey() + "\n" +
                                "Использовано трафика: " + trafficByUser + " GB";
                        execute(new SendMessage(String.valueOf(chatId), showUserAnswer));
                    }
                } catch (TelegramApiException e) {
                    log.error("Error yopta: " + e.getMessage());
                }
            } else {
                try {
                    execute(new SendMessage(String.valueOf(chatId), "Вы не зарегистрировались. Используйте команду /start"));
                } catch (TelegramApiException e) {
                    log.error("Error yopta: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            try {
                execute(new SendMessage(String.valueOf(chatId), "Что то пошло не так."));
            } catch (TelegramApiException e2) {
                log.error("Error yopta: " + e2.getMessage());
            }
        }
    }

    private void showUserKey(Message message) {
        var chatId = message.getChatId();

        try {
            User user = userRepository.findById(message.getFrom().getId()).get();
            String showUserKeyAnswer = user.getTokenKey();

            if (showUserKeyAnswer != null) {
                startCommand(chatId, showUserKeyAnswer);
            } else {
                try {
                    execute(new SendMessage(String.valueOf(chatId), "Что то пошло не так."));
                } catch (TelegramApiException e) {
                    log.error("1Error yopta: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            try {
                execute(new SendMessage(String.valueOf(chatId), "Вы не зарегистрировались. Используйте команду /start"));
            } catch (TelegramApiException e2) {
                log.error("2Error yopta: " + e2.getMessage());
            }
        }
    }

    public void sendUserMessageAboutTrafficUsed() throws IOException {
        try {
            Map<Long, Double> list = new LinkedHashMap<>();

            userRepository.findAll().forEach(el -> {
                if (el.getTrafficUsed() != null) {
                    list.put(el.getChatId(), el.getTrafficUsed());
                }
            });

            for (Long chatId : list.keySet()) {
                startCommand(chatId, "Использовано трафика: " + list.get(chatId) + " GB");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveInDatabaseTrafficUsedByUser() {
        try {
            Map<String, Long> listOfActiveUsers = new HashMap<>();

            userRepository.findAll().forEach(el -> {
                try {
                    if (getUsedTrafficByUser().containsKey(el.getToken())) {
                        listOfActiveUsers.put(el.getToken(), el.getChatId());
                    }
                } catch (IOException e) {
                    System.out.println("Ошибка в saveInDatabaseTrafficUsedByUser(): " + e.getMessage());
                }
            });

            for (String activeToken : listOfActiveUsers.keySet()) {
                User user = userRepository.findById(listOfActiveUsers.get(activeToken)).get();
                Map<String, Double> getUsedTrafficByUserInGb = new LinkedHashMap<>();

                getUsedTrafficByUser().entrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .forEachOrdered(e -> getUsedTrafficByUserInGb.put(e.getKey(), Math.round((e.getValue() / 1_073_741_824.0) * 100.0) / 100.0));

                user.setTrafficUsed(getUsedTrafficByUserInGb.get(activeToken));
                userRepository.save(user);
            }
        } catch (Exception e) {
            System.out.println("Ошибка при сохранении в БД трафка пользователей: " + e.getMessage() + "\n" + e.getStackTrace());
        }
    }

    private Map<String, Long> getUsedTrafficByUser() throws IOException {
        Map<String, Long> trafficData = new LinkedHashMap<>();

        HttpsURLConnection connection = (HttpsURLConnection) new URL(apiUrl).openConnection();
        connection.setRequestMethod("GET");
        removeSSLVerifier(connection);

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonResponse = objectMapper.readTree(connection.getInputStream());

        JsonNode userTraffic = jsonResponse.get("bytesTransferredByUserId");
        if (userTraffic != null) {
            for (Iterator<String> it = userTraffic.fieldNames(); it.hasNext(); ) {
                String userId = it.next();
                long bytesTransferred = userTraffic.get(userId).asLong();
                trafficData.put(String.valueOf(userId), bytesTransferred);
            }
        }

        return trafficData;
    }

    private void scheduleDailyTask(int targetHour, int targetMinute) {
        try {
            ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

            Runnable task = () -> {
                System.out.println("Запуск задачи: " + LocalDateTime.now());
                try {
                    saveInDatabaseTrafficUsedByUser();
                    sendUserMessageAboutTrafficUsed();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
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

        if (now.compareTo(nextRun) > 0) { // Если текущее время уже после 10:00, берем завтра
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();
    }

    private void removeSSLVerifier(@NonNull HttpsURLConnection connection) {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1) {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1) {
                    }
                }
        };

        SSLContext sc = null;
        try {
            sc = SSLContext.getInstance("SSL");
        } catch (NoSuchAlgorithmException e) {
            System.out.println(e.getMessage());
        }
        try {
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
        } catch (KeyManagementException e) {
            System.out.println(e.getMessage());
        }
        connection.setSSLSocketFactory(sc.getSocketFactory());

        HostnameVerifier validHosts = (arg0, arg1) -> true;

        connection.setHostnameVerifier(validHosts);
    }
}
