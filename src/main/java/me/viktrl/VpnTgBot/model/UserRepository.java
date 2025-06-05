package me.viktrl.VpnTgBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Map;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u.username FROM users u WHERE u.token IS NULL")
    List<String> listOfInactiveUsers();

    @Query("SELECT u.username, u.trafficUsed FROM users u WHERE u.trafficUsed IS NOT NULL")
    List<Object[]> listOfActiveUsers();

    @Query("SELECT u.chatId FROM users u")
    List<Long> listOfRegisteredUsers();
}
