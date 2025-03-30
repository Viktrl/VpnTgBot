package me.viktrl.VpnTgBot.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@Data
@PropertySource("application.properties")
public class BotConfig {
    @Value( "${bot.name}")
    String botName;

    @Value( "${bot.token}")
    String botToken;

    @Value("${outline.api.url}")
    String outlineApiUrl;

    @Value("${admin}")
    String admin;
}