package me.viktrl.VpnTgBot.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

import java.sql.Timestamp;

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

    private Double trafficUsed;

    public Long getChatId() {
        return chatId;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getUsername() {
        return username;
    }

    public Timestamp getRegisteredAt() {
        return registeredAt;
    }

    public String getToken() {
        return token;
    }

    public String getTokenKey() {
        return tokenKey;
    }

    public Double getTrafficUsed() {
        return trafficUsed;
    }

    public void setChatId(Long chatId) {
        this.chatId = chatId;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setRegisteredAt(Timestamp registeredAt) {
        this.registeredAt = registeredAt;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public void setTokenKey(String tokenKey) {
        this.tokenKey = tokenKey;
    }

    public void setTrafficUsed(Double trafficUsed) {
        this.trafficUsed = trafficUsed;
    }

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
                ", trafficUsed=" + trafficUsed +
                '}';
    }
}
