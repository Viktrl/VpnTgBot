package me.viktrl.VpnTgBot.model;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;

public interface PromocodesRepository extends CrudRepository<Promocodes, Long> {
    @Query("SELECT p.id FROM Promocodes p WHERE p.userId = :chatId")
    Long findPromocodeByUserId(@Param("chatId") Long chatId);
}
