package me.viktrl.VpnTgBot.service;

import me.viktrl.VpnTgBot.model.Promocodes;
import me.viktrl.VpnTgBot.model.PromocodesRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class PromoCodeGenerator {

    private final PromocodesRepository promocodesRepo;
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final SecureRandom random = new SecureRandom();
    private static final int CODE_LENGTH = 6;

    public PromoCodeGenerator(PromocodesRepository repo) {
        this.promocodesRepo = repo;
    }

    public String generateCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }

    public void generateAndSaveUniqueCode(Long userId) {
        if (promocodesRepo.findPromocodeByUserId(userId) == null) {
            String code = generateCode(CODE_LENGTH);
            Promocodes promo = new Promocodes();
            promo.setCode(code);
            promo.setUserId(userId);
            promocodesRepo.save(promo);
        }
    }
}
