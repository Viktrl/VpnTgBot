package me.viktrl.VpnTgBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Setter
@Getter
@Entity(name = "users")
public class User {

    public User() {
    }

    @Id
    private Long chatId;

    private String firstName;

    private String lastName;

    private String username;

    private Timestamp registeredAt;

    private String token;

    private String tokenKey;

    private Timestamp tokenCreatedAt;

    private Double trafficUsed;

    @Override
    public String toString() {
        return "User{" +
                "chatId=" + chatId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", username='" + username + '\'' +
                ", registeredAt=" + registeredAt +
                ", token='" + token + '\'' +
                ", tokenKey='" + tokenKey + '\'' +
                ", tokenCreatedAt=" + tokenCreatedAt +
                ", trafficUsed=" + trafficUsed +
                '}';
    }
}
