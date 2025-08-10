package com.footArena.booking.security.services;

import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.exceptions.UnauthorizedAccessException;
import com.footArena.booking.security.entities.RefreshToken;
import com.footArena.booking.security.repositories.RefreshTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class RefreshTokenService {

    private static final Logger logger = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int MAX_REFRESH_TOKENS_PER_USER = 5;

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtService = jwtService;
    }

    public RefreshToken createRefreshToken(User user, String tokenValue, boolean rememberMe,
                                           String deviceInfo, String ipAddress) {
        logger.info("Creating refresh token for user: {}", user.getEmail());

        // Nettoyer les anciens tokens si trop nombreux
        cleanupUserTokens(user.getId());

        LocalDateTime expiresAt = rememberMe
                ? LocalDateTime.now().plusDays(30)
                : LocalDateTime.now().plusDays(7);

        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setTokenHash(jwtService.hashToken(tokenValue));
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(expiresAt);
        refreshToken.setDeviceInfo(deviceInfo);
        refreshToken.setIpAddress(ipAddress);

        RefreshToken savedToken = refreshTokenRepository.save(refreshToken);
        logger.info("Refresh token created with ID: {}", savedToken.getId());

        return savedToken;
    }

    /**
     * Trouve un refresh token par son hash
     */
    @Transactional(readOnly = true)
    public Optional<RefreshToken> findByTokenValue(String tokenValue) {
        String tokenHash = jwtService.hashToken(tokenValue);
        return refreshTokenRepository.findByTokenHash(tokenHash);
    }

    public RefreshToken validateAndUseRefreshToken(String tokenValue) {
        logger.debug("Validating refresh token");

        String tokenHash = jwtService.hashToken(tokenValue);
        RefreshToken refreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new UnauthorizedAccessException("Invalid refresh token"));

        if (!refreshToken.isValid()) {
            logger.warn("Invalid refresh token used for user: {}", refreshToken.getUser().getEmail());
            throw new UnauthorizedAccessException("Refresh token is expired or revoked");
        }

        refreshToken.updateLastUsed();
        refreshTokenRepository.save(refreshToken);

        logger.info("Refresh token validated for user: {}", refreshToken.getUser().getEmail());
        return refreshToken;
    }

    public void revokeRefreshToken(String tokenValue) {
        logger.info("Revoking refresh token");

        String tokenHash = jwtService.hashToken(tokenValue);
        Optional<RefreshToken> tokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);

        if (tokenOpt.isPresent()) {
            RefreshToken token = tokenOpt.get();
            token.revoke();
            refreshTokenRepository.save(token);
            logger.info("Refresh token revoked for user: {}", token.getUser().getEmail());
        }
    }

    public void revokeAllUserTokens(UUID userId) {
        logger.info("Revoking all refresh tokens for user: {}", userId);
        refreshTokenRepository.revokeAllTokensByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<RefreshToken> getUserValidTokens(UUID userId) {
        return refreshTokenRepository.findValidTokensByUser(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public long countUserValidTokens(UUID userId) {
        return refreshTokenRepository.countValidTokensByUser(userId);
    }

    /**
     * Nettoie les anciens tokens d'un utilisateur
     */
    private void cleanupUserTokens(UUID userId) {
        List<RefreshToken> userTokens = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(userId);

        if (userTokens.size() >= MAX_REFRESH_TOKENS_PER_USER) {
            // Garder les 4 plus récents et supprimer les autres
            List<RefreshToken> tokensToRevoke = userTokens.subList(MAX_REFRESH_TOKENS_PER_USER - 1, userTokens.size());
            tokensToRevoke.forEach(RefreshToken::revoke);
            refreshTokenRepository.saveAll(tokensToRevoke);

            logger.info("Cleaned up {} old refresh tokens for user: {}", tokensToRevoke.size(), userId);
        }
    }

    /**
     * Rotation d'un refresh token (remplace l'ancien par un nouveau)
     */
    public RefreshToken rotateRefreshToken(RefreshToken oldToken, String newTokenValue,
                                           String deviceInfo, String ipAddress) {
        logger.info("Rotating refresh token for user: {}", oldToken.getUser().getEmail());

        oldToken.revoke();
        refreshTokenRepository.save(oldToken);

        // Créer le nouveau token avec les mêmes paramètres
        boolean rememberMe = oldToken.getExpiresAt().isAfter(LocalDateTime.now().plusDays(8));

        return createRefreshToken(
                oldToken.getUser(),
                newTokenValue,
                rememberMe,
                deviceInfo,
                ipAddress
        );
    }

    /**
     * Nettoyage automatique des tokens expirés (scheduled)
     */
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h du matin
    public void cleanupExpiredTokens() {
        logger.info("Starting cleanup of expired refresh tokens");

        refreshTokenRepository.cleanupExpiredTokens(LocalDateTime.now());

        refreshTokenRepository.findAll().stream()
                .filter(token -> token.isRevoked() &&
                        token.getCreatedAt().isBefore(LocalDateTime.now().minusDays(30)))
                .forEach(refreshTokenRepository::delete);

        logger.info("Cleanup of expired refresh tokens completed");
    }

    /**
     * Détecte une utilisation suspecte de refresh token
     */
    public boolean detectSuspiciousActivity(RefreshToken token, String currentIpAddress) {
        // Vérifier si l'IP a changé
        if (token.getIpAddress() != null && !token.getIpAddress().equals(currentIpAddress)) {
            logger.warn("IP address changed for refresh token. Old: {}, New: {}",
                    token.getIpAddress(), currentIpAddress);
            return true;
        }

        // Vérifier l'utilisation fréquente
        if (token.getLastUsedAt() != null &&
                token.getLastUsedAt().isAfter(LocalDateTime.now().minusMinutes(1))) {
            logger.warn("Refresh token used very recently, possible attack");
            return true;
        }

        return false;
    }

    /**
     * Statistiques des refresh tokens
     */
    @Transactional(readOnly = true)
    public RefreshTokenStats getTokenStats(UUID userId) {
        List<RefreshToken> allTokens = refreshTokenRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long validTokens = refreshTokenRepository.countValidTokensByUser(userId);

        return new RefreshTokenStats(
                allTokens.size(),
                (int) validTokens,
                allTokens.stream().filter(RefreshToken::isRevoked).count()
        );
    }

    // Classe interne pour les statistiques
    public static class RefreshTokenStats {
        private final int totalTokens;
        private final int validTokens;
        private final long revokedTokens;

        public RefreshTokenStats(int totalTokens, int validTokens, long revokedTokens) {
            this.totalTokens = totalTokens;
            this.validTokens = validTokens;
            this.revokedTokens = revokedTokens;
        }

        // Getters
        public int getTotalTokens() {
            return totalTokens;
        }

        public int getValidTokens() {
            return validTokens;
        }

        public long getRevokedTokens() {
            return revokedTokens;
        }
    }
}