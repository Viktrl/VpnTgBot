package me.viktrl.VpnTgBot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import me.dynomake.outline.OutlineWrapper;
import me.dynomake.outline.gson.GsonUtil;
import me.dynomake.outline.implementation.model.Response;
import me.dynomake.outline.model.OutlineServer;
import me.viktrl.VpnTgBot.config.BotConfig;
import me.viktrl.VpnTgBot.model.User;
import me.viktrl.VpnTgBot.model.UserRepository;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
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
import java.util.stream.Collectors;

import org.json.simple.JSONObject;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    OutlineWrapper outlineWrapper = OutlineWrapper.create("https://217.78.239.38:33710/lXJ6H_DXmIg9yuOCLTKKiA");
    private static Map<String, Long> previousData = new HashMap<>(); // Храним прошлые данные
    private static final String DATA_FILE = "traffic_data.json"; // Файл для сохранения данных

    public TelegramBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;

        List<BotCommand> listCommand = new ArrayList<>();
        listCommand.add(new BotCommand("/start", "Войти"));
        listCommand.add(new BotCommand("/registerkey", "Создать ВПН"));
        listCommand.add(new BotCommand("/myaccount", "Мои данные"));
        listCommand.add(new BotCommand("/mykey", "Мой ключ доступа"));
        listCommand.add(new BotCommand("/help", "Инструкция"));

        try {
            this.execute(new SetMyCommands(listCommand, new BotCommandScopeDefault(), "en"));
            scheduleDailyTask(14,40);
        } catch (TelegramApiException e) {
            log.error("Error yopta: " + e.getMessage());
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
                case "/registerkey":
                    registerKey(update.getMessage());
                    break;
                case "/myaccount":
                    showUserAccount(update.getMessage());
                    break;
                case "/mykey":
                    showUserKey(update.getMessage());
                    break;
                case "/help":
                    startCommand(chatId, "1. Скопируйте ключ доступа /mykey\n\n" +
                            "2. Скачайте и установите подходящее вашему устройству приложение Outline: \n" +
                            "iOS: https://itunes.apple.com/app/outline-app/id1356177741\n" +
                            "Android: https://play.google.com/store/apps/details?id=org.outline.android.client\n" +
                            "macOS: https://itunes.apple.com/app/outline-app/id1356178125\n" +
                            "Windows: https://s3.amazonaws.com/outline-releases/client/windows/stable/Outline-Client.exe\n" +
                            "Linux: https://s3.amazonaws.com/outline-releases/client/linux/stable/Outline-Client.AppImage\n" +
                            "Дополнительная ссылка для Android: https://s3.amazonaws.com/outline-releases/client/android/stable/Outline-Client.apk\n\n" +
                            "3. Откройте клиент Outline. Если ваш ключ доступа определился автоматически, нажмите \"Подключиться\". Если этого не произошло, вставьте ключ в поле и нажмите \"Подключиться\".\n\n" +
                            "Теперь у вас есть доступ к свободному интернету. Чтобы убедиться, что вы подключились к серверу, зайдите на 2ip.ru, и проверьте IP.");
                    break;
//                case "/sendusermessage":
//                    sendUserMessageAboutTrafficUsed(update.getMessage());
//                    break;
                default:
                    startCommand(chatId, "Этой команды не существует");
            }
        }
    }

    private void startCommand(long chatId, String name) {
        try {
            execute(new SendMessage(String.valueOf(chatId), name));
        } catch (TelegramApiException e) {
            log.error("Error yopta: " + e.getMessage());
        }
    }

    private void registerUser(Message message) {
        User user = new User();
        var chatId = message.getChatId();

        if (userRepository.findById(message.getFrom().getId()).isEmpty()) {
            var chat = message.getChat();

            user.setChatId(chatId);
            user.setFirstName(message.getFrom().getFirstName());
            user.setLastName(message.getFrom().getLastName());
            user.setUsername(message.getFrom().getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("User registered: " + user);

            String AnswerUserSuccessCreated = "Добрый день! Вы успешно зарегистрировались.\nВаш логин в системе: " + user.getUsername() + "\n\nСоздайте токен используя команду /registerkey";
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
            var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();

            if (savedUserFromDb.getToken() == null) {
                int newKeyId = outlineWrapper.generateKey().id;

                savedUserFromDb.setToken(String.valueOf(outlineWrapper.getKey(newKeyId).id));
                savedUserFromDb.setTokenKey(outlineWrapper.getKey(newKeyId).accessUrl);
                userRepository.save(savedUserFromDb);
//                log.info("User created VPN: " + user);

                String AnswerVpnSuccessCreated = "Бесплатный ВПН создан. Ключ:\n" + savedUserFromDb.getTokenKey() + "\n\nИспользуйте команду /help чтобы получить инструкцию к легкой установке и настройке ВПН";
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
                execute(new SendMessage(String.valueOf(chatId), "Вы не зарегистрировались.\n" +
                        "Используйте команду /start"));
            } catch (TelegramApiException e2) {
                log.error("Error yopta: " + e2.getMessage());
            }
        }
    }

    private void showUserAccount(Message message) {
        var chatId = message.getChatId();
        var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();

        try {
            if (!userRepository.findById(message.getFrom().getId()).isEmpty()) {
                try {
                    if (message.getChat().getUserName().equals("unmaskked")) {
                        Map<String, Long> rawFetchTrafficData = fetchTrafficData();
                        Map<String, Double> fetchTrafficDataInGb = new LinkedHashMap<>();

                        rawFetchTrafficData.entrySet()
                                .stream()
                                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                                .forEachOrdered(e -> fetchTrafficDataInGb.put(e.getKey(), Double.valueOf(e.getValue())));

                        for (Map.Entry<String, Long> entry : rawFetchTrafficData.entrySet()) {
                            String userId = entry.getKey();
                            double trafficInGb = entry.getValue() / 1_073_741_824.0; // Конвертация в ГБ
                            fetchTrafficDataInGb.put(userId, Math.round(trafficInGb * 100) / 100.0);
                        }

                        String prettyJson = new GsonBuilder().setPrettyPrinting().create().toJson(fetchTrafficDataInGb);
                        String showAdminAnswer = "Логин: " + savedUserFromDb.getUsername() + "\n" +
                                "ID: " + savedUserFromDb.getToken() + "\n" +
                                "Ключ для ВПН: " + savedUserFromDb.getTokenKey() + "\n" +
                                "Использовано трафика:\n" + prettyJson;
                        execute(new SendMessage(String.valueOf(chatId), showAdminAnswer));
                    } else {
                        Long trafficByUser = fetchTrafficData().get(savedUserFromDb.getToken());
                        Double trafficByUserInGb = trafficByUser / 1_073_741_824.0;
                        String showUserAnswer = "Логин: " + savedUserFromDb.getUsername() + "\n" +
                                "ID: " + savedUserFromDb.getToken() + "\n" +
                                "Ключ для ВПН: " + savedUserFromDb.getTokenKey() + "\n" +
                                "Использовано трафика: " + String.format("%.2f GB", trafficByUserInGb);
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
        var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();
        String showUserKeyAnswer = savedUserFromDb.getTokenKey();

        try {
            if (!userRepository.findById(message.getFrom().getId()).isEmpty()) {
                startCommand(chatId, showUserKeyAnswer);
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

    private Map<String, Long> fetchTrafficData() throws IOException {
        Map<String, Long> trafficData = new LinkedHashMap<>();

        HttpsURLConnection connection = (HttpsURLConnection) new URL("https://217.78.239.38:33710/lXJ6H_DXmIg9yuOCLTKKiA/metrics/transfer").openConnection();
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

    public void sendUserMessageAboutTrafficUsed() throws IOException {
        var chatId = message.getChatId();
        var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();
        Map<String, Long> currentData = fetchTrafficData();

        try {
            loadPreviousData();
            long previous = previousData.getOrDefault("56", 0L);
            long current = currentData.get("56");
            long delta = current - previous;

            double deltaInGb = delta / 1_073_741_824.0;

            if (deltaInGb > 5.0) {
                startCommand(245344798, String.format("Ваше потребление трафика за сутки: %.2f GB", deltaInGb));
            } else {
                System.out.println("sout = 5 gb nety");
            }

            previousData = currentData;
//            savePreviousData();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void scheduleDailyTask(int targetHour, int targetMinute) {
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

        Runnable task = () -> {
            System.out.println("Запуск задачи: " + LocalDateTime.now());
            try {
                sendUserMessageAboutTrafficUsed();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };

        long initialDelay = calculateInitialDelay(targetHour, targetMinute);
        long period = 24 * 60 * 60; // 24 часа в секундах

        scheduler.scheduleAtFixedRate(task, initialDelay, period, TimeUnit.SECONDS);
    }

    private long calculateInitialDelay(int targetHour, int targetMinute) {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Europe/Moscow"));
        ZonedDateTime nextRun = now.withHour(targetHour).withMinute(targetMinute).withSecond(0);

        if (now.compareTo(nextRun) > 0) { // Если текущее время уже после 10:00, берем завтра
            nextRun = nextRun.plusDays(1);
        }

        return Duration.between(now, nextRun).getSeconds();
    }

    private static void savePreviousData() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.writeValue(new File(DATA_FILE), previousData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void loadPreviousData() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            File file = new File(DATA_FILE);
            if (file.exists()) {
                previousData = objectMapper.readValue(file, new TypeReference<Map<String, Long>>() {
                });
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void removeSSLVerifier(@NonNull HttpsURLConnection connection) {
        TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                        return null;
                    }

                    @Override
                    public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {
                    }

                    @Override
                    public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                            throws CertificateException {
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
