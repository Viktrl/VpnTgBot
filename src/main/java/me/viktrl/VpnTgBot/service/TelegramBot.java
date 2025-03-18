package me.viktrl.VpnTgBot.service;

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
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {
    @Autowired
    private UserRepository userRepository;
    final BotConfig config;

    public TelegramBot(BotConfig config) {
        super(config.getBotToken());
        this.config = config;

        List<BotCommand> listCommand = new ArrayList<>();
        listCommand.add(new BotCommand("/start", "Войти"));
        listCommand.add(new BotCommand("/registerkey", "Создать ВПН"));
        listCommand.add(new BotCommand("/myaccount", "Мои данные"));
        listCommand.add(new BotCommand("/help", "Инструкция"));
//        listCommand.add(new BotCommand("/settings", "Настройки"));

        try {
            this.execute(new SetMyCommands(listCommand, new BotCommandScopeDefault(), "en"));
        } catch (TelegramApiException e) {
            log.error("Error yopta: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

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
                default: startCommand(chatId, "Этой команды не существует");
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

        if(userRepository.findById(message.getFrom().getId()).isEmpty()) {
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

            if(savedUserFromDb.getToken() == null) {
                OutlineWrapper outlineWrapper = OutlineWrapper.create("https://217.78.239.38:33710/lXJ6H_DXmIg9yuOCLTKKiA");
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

        try {
            var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();
            if(!userRepository.findById(message.getFrom().getId()).isEmpty()) {
                String showUserAnswer = "Логин: " + savedUserFromDb.getUsername() + "\n" +
                        "ID: " + savedUserFromDb.getToken() + "\n" +
                        "Ключ для ВПН: " + savedUserFromDb.getTokenKey();
                try {
                    execute(new SendMessage(String.valueOf(chatId), showUserAnswer));
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
                execute(new SendMessage(String.valueOf(chatId), "Вы не зарегистрировались.\n" +
                        "Используйте команду /start"));
            } catch (TelegramApiException e2) {
                log.error("Error yopta: " + e2.getMessage());
            }
        }

    }

    private void showUserKey(Message message) {
        var chatId = message.getChatId();
        var savedUserFromDb = userRepository.findById(message.getFrom().getId()).get();
        String showUserKeyAnswer = savedUserFromDb.getTokenKey();

        startCommand(chatId, showUserKeyAnswer); //TODO обернуть в try catch
    }
}
