package me.viktrl.VpnTgBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Map;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u.username FROM users u WHERE u.token IS NOT NULL and u.trafficUsed IS NULL")
    List<String> listOfInactiveUsers();

    @Query("SELECT u.username, u.trafficUsed FROM users u WHERE u.trafficUsed IS NOT NULL")
    List<Object[]> listOfActiveUsers();

    @Query("SELECT u.chatId FROM users u")
    List<Long> listOfRegisteredUsers();

    @Query("SELECT tokenKey FROM users WHERE chatId = :chatId")
    String queryShowServers(@Param("chatId") Long chatId);
}
