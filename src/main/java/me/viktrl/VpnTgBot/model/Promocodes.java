package me.viktrl.VpnTgBot.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "promocodes", uniqueConstraints = @UniqueConstraint(columnNames = "code"))
public class Promocodes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String code;

    @Column(nullable = false)
    private Long userId;

    @Override
    public String toString() {
        return "Promocodes{" +
                "id=" + id +
                ", code='" + code + '\'' +
                ", userId=" + userId +
                '}';
    }
}
