package me.viktrl.VpnTgBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends CrudRepository<User, Long> {
    @Query("SELECT u.username FROM User u WHERE u.token IS NULL")
    List<String> listOfInactiveUsers();
}
