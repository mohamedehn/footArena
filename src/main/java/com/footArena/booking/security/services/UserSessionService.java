package com.footArena.booking.security.services;

import com.footArena.booking.domain.entities.User;
import com.footArena.booking.security.entities.UserSession;
import com.footArena.booking.security.repositories.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
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
public class UserSessionService {

    private static final Logger logger = LoggerFactory.getLogger(UserSessionService.class);

    private final UserSessionRepository sessionRepository;

    public UserSessionService(UserSessionRepository sessionRepository) {
        this.sessionRepository = sessionRepository;
    }

    /**
     * Crée une nouvelle session utilisateur
     */
    public UserSession createSession(User user, String sessionToken, HttpServletRequest request) {
        logger.info("Creating session for user: {}", user.getEmail());

        String deviceInfo = extractDeviceInfo(request);
        String ipAddress = getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        UserSession session = new UserSession(user, sessionToken, deviceInfo, ipAddress, userAgent);

        // Géolocalisation basée sur IP
        session.setLocation(getLocationFromIp(ipAddress));

        UserSession savedSession = sessionRepository.save(session);
        logger.info("Session created with ID: {}", savedSession.getId());

        return savedSession;
    }

    @Transactional(readOnly = true)
    public Optional<UserSession> findBySessionToken(String sessionToken) {
        return sessionRepository.findBySessionToken(sessionToken);
    }

    public void updateSessionActivity(String sessionToken) {
        Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.updateActivity();
            sessionRepository.save(session);
        }
    }

    public void deactivateSession(String sessionToken) {
        Optional<UserSession> sessionOpt = sessionRepository.findBySessionToken(sessionToken);
        if (sessionOpt.isPresent()) {
            UserSession session = sessionOpt.get();
            session.deactivate();
            sessionRepository.save(session);
            logger.info("Session deactivated: {}", sessionToken);
        }
    }

    public void deactivateAllUserSessions(UUID userId) {
        logger.info("Deactivating all sessions for user: {}", userId);
        sessionRepository.deactivateAllSessionsByUser(userId);
    }

    @Transactional(readOnly = true)
    public List<UserSession> getUserActiveSessions(UUID userId) {
        return sessionRepository.findActiveSessionsByUser(userId, LocalDateTime.now());
    }

    @Transactional(readOnly = true)
    public List<UserSession> getUserSessionHistory(UUID userId, int limit) {
        List<UserSession> sessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return sessions.size() > limit ? sessions.subList(0, limit) : sessions;
    }

    @Transactional(readOnly = true)
    public long countUserActiveSessions(UUID userId) {
        return sessionRepository.countActiveSessionsByUser(userId);
    }


    @Scheduled(cron = "0 30 * * * *") // Toutes les 30 minutes
    public void cleanupExpiredSessions() {
        logger.info("Starting cleanup of expired sessions");
        sessionRepository.cleanupExpiredSessions(LocalDateTime.now());
        logger.info("Cleanup of expired sessions completed");
    }

    public boolean detectSuspiciousLogin(User user, HttpServletRequest request) {
        String currentIp = getClientIpAddress(request);
        LocalDateTime since = LocalDateTime.now().minusHours(24);

        List<UserSession> recentSessions = sessionRepository.findRecentSessionsByIp(currentIp, since);

        // Si plus de 10 tentatives depuis cette IP en 24h
        if (recentSessions.size() > 10) {
            logger.warn("Suspicious activity detected from IP: {}", currentIp);
            return true;
        }

        // Vérifier si l'utilisateur se connecte depuis une nouvelle localisation
        List<UserSession> userSessions = getUserActiveSessions(user.getId());
        if (!userSessions.isEmpty()) {
            String currentLocation = getLocationFromIp(currentIp);
            boolean newLocation = userSessions.stream()
                    .noneMatch(session -> currentLocation.equals(session.getLocation()));

            if (newLocation) {
                logger.info("User connecting from new location: {}", currentLocation);
                // Ici, on pourrait envoyer un email de notification
            }
        }

        return false;
    }

    /**
     * Extrait les informations de l'appareil depuis la requête
     */
    private String extractDeviceInfo(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent == null) return "Unknown Device";

        // Détection simplifiée du type d'appareil
        if (userAgent.contains("Mobile")) return "Mobile Device";
        if (userAgent.contains("Tablet")) return "Tablet";
        if (userAgent.contains("Windows")) return "Windows Desktop";
        if (userAgent.contains("Mac")) return "Mac Desktop";
        if (userAgent.contains("Linux")) return "Linux Desktop";

        return "Unknown Device";
    }

    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Obtient la localisation basée sur l'IP
     */
    private String getLocationFromIp(String ipAddress) {
        // En production, utiliser un service de géolocalisation IP
        if (ipAddress.startsWith("127.") || ipAddress.equals("0:0:0:0:0:0:0:1")) {
            return "Local";
        }

        // Ici, intégrer un service comme MaxMind GeoIP, ipapi.co, etc.
        return "Unknown Location";
    }

    /**
     * Statistiques des sessions
     */
    @Transactional(readOnly = true)
    public SessionStats getSessionStats(UUID userId) {
        List<UserSession> allSessions = sessionRepository.findByUserIdOrderByCreatedAtDesc(userId);
        long activeSessions = sessionRepository.countActiveSessionsByUser(userId);

        return new SessionStats(
                allSessions.size(),
                (int) activeSessions,
                allSessions.stream().filter(s -> !s.isActive()).count()
        );
    }

    // Classe interne pour les statistiques
    public static class SessionStats {
        private final int totalSessions;
        private final int activeSessions;
        private final long inactiveSessions;

        public SessionStats(int totalSessions, int activeSessions, long inactiveSessions) {
            this.totalSessions = totalSessions;
            this.activeSessions = activeSessions;
            this.inactiveSessions = inactiveSessions;
        }

        public int getTotalSessions() {
            return totalSessions;
        }

        public int getActiveSessions() {
            return activeSessions;
        }

        public long getInactiveSessions() {
            return inactiveSessions;
        }
    }
}