package me.viktrl.VpnTgBot.service.Adminka;

import me.viktrl.VpnTgBot.model.User;
import me.viktrl.VpnTgBot.model.UserRepository;
import me.viktrl.VpnTgBot.service.Requests;
import me.viktrl.VpnTgBot.service.TelegramBot;
import org.springframework.beans.factory.annotation.Autowired;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.*;

public class AFunc {
    @Autowired
    UserRepository userRepository;

    public void adminPanelCommand(Message message, String admin) {
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

    public void showUsersListToKeyDelete(Message message, String admin) {
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

    public void deleteKeyByUsername(Long chatId, String username) {
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
}
