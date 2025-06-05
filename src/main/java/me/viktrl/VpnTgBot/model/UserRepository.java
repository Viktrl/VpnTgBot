package me.viktrl.VpnTgBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u.username FROM users u WHERE u.token IS NULL")
    List<String> listOfInactiveUsers();
}
