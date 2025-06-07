package me.viktrl.VpnTgBot.service;

import me.viktrl.VpnTgBot.model.User;
import me.viktrl.VpnTgBot.model.UserRepository;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Scheduler {
    UserRepository userRepository;
    TelegramBot telegramBot;

    public void sendUserMessageAboutTrafficUsed() {
        try {
            Map<Long, Double> activeUsersMapByChatIdAndTrafficUsed = new LinkedHashMap<>();

            userRepository.findAll().forEach(el -> {
                if (el.getTrafficUsed() != null) {
                    activeUsersMapByChatIdAndTrafficUsed.put(el.getChatId(), el.getTrafficUsed());
                }
            });

            for (Long chatId : activeUsersMapByChatIdAndTrafficUsed.keySet()) {
                telegramBot.sendMessage(chatId, "Использовано трафика: " + activeUsersMapByChatIdAndTrafficUsed.get(chatId) + " GB");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void saveInDatabaseTrafficUsedByUser() {
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

    public void scheduleDailyTask(int targetHour, int targetMinute) {
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
