package com.footArena.booking.security.services;

import com.footArena.booking.domain.model.entity.BlacklistedToken;
import com.footArena.booking.domain.repository.BlacklistedTokenRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class BlackListTokenService {

    private final BlacklistedTokenRepository blacklistedTokenRepository;
    private final JwtDecoder jwtDecoder;

    @Autowired
    public BlackListTokenService(BlacklistedTokenRepository blacklistedTokenRepository, JwtDecoder jwtDecoder) {
        this.blacklistedTokenRepository = blacklistedTokenRepository;
        this.jwtDecoder = jwtDecoder;
    }

    public boolean isBlacklistTokenExpired(Instant expiryDate, BlacklistedToken blacklistedToken) {
        if (Instant.now().isAfter(expiryDate)) {
            blacklistedToken.setBlackListed(true);
        }
        return blacklistedToken.isBlackListed();
    }

    public void deleteToken(String token) {
        BlacklistedToken blacklistedToken = blacklistedTokenRepository.findByToken(token);
        if (blacklistedToken != null) {
            blacklistedTokenRepository.delete(blacklistedToken);
        }
    }

    @Scheduled(cron = "0 0 11 * * *")
    public void checkBlacklistedTokens() {
        List<BlacklistedToken> tokens = blacklistedTokenRepository.findAll();
        Instant now = Instant.now();

        for (BlacklistedToken token : tokens) {
            try {
                Jwt jwt = this.jwtDecoder.decode(token.getToken());
                Map<String, Object> claims = jwt.getClaims();
                Instant expiryDate = (Instant) claims.get("exp");
                System.out.println(expiryDate);
                if (!token.isBlackListed() && (now.isAfter(token.getExpiresAt()) || now.isAfter(expiryDate))) {
                    token.setBlackListed(true);
                    blacklistedTokenRepository.save(token);
                }
            } catch (org.springframework.security.oauth2.jwt.JwtValidationException e) {
                if (e.getMessage().contains("Jwt expired")) {
                    token.setBlackListed(true);
                    blacklistedTokenRepository.save(token);
                }
            }
        }
    }

    public void saveToken(String token, Instant expiryDate, UUID userId) {
        BlacklistedToken blacklistedToken = new BlacklistedToken();
        blacklistedToken.setToken(token);
        blacklistedToken.setExpiresAt(expiryDate);
        blacklistedToken.setUserId(userId);
        blacklistedToken.setBlackListed(false);
        blacklistedTokenRepository.save(blacklistedToken);
    }
}