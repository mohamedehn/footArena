package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.*;
import com.footArena.booking.domain.enums.NotificationType;
import com.footArena.booking.domain.repositories.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class NotificationService {

    private static final Logger logger = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notificationRepository;
    private final EmailService emailService;

    public NotificationService(NotificationRepository notificationRepository,
                               EmailService emailService) {
        this.notificationRepository = notificationRepository;
        this.emailService = emailService;
    }

    /**
     * Notifie la création d'un match
     */
    @Async
    public void notifyMatchCreated(Match match) {
        logger.info("Notifying match creation: {}", match.getTitle());

        // Pour l'instant, juste logger
        // TODO: Implémenter la notification push/email

        String message = String.format(
                "Nouveau match créé : %s\nType: %s\nDate: %s\nLieu: %s",
                match.getTitle(),
                match.getMatchType(),
                match.getSlot().getStartTime(),
                match.getField().getName()
        );

        // Envoyer un email au créateur
        emailService.sendMatchCreatedEmail(match.getCreator(), match);
    }

    /**
     * Notifie qu'un joueur a rejoint le match
     */
    @Async
    public void notifyPlayerJoined(Match match, User newPlayer) {
        logger.info("Notifying player joined: {} to match {}", newPlayer.getFullName(), match.getTitle());

        // Notifier le créateur du match
        Notification notification = new Notification(
                match.getCreator(),
                NotificationType.PLAYER_JOINED,
                "Nouveau joueur dans votre match",
                String.format("%s a rejoint votre match '%s'", newPlayer.getFullName(), match.getTitle())
        );
        notification.setRelatedEntityId(match.getId());
        notification.setRelatedEntityType("MATCH");
        notification.setExpiresAt(match.getSlot().getStartTime());

        notificationRepository.save(notification);

        // Notifier tous les autres joueurs
        match.getPlayers().stream()
                .filter(mp -> !mp.getUser().getId().equals(newPlayer.getId()))
                .forEach(mp -> {
                    Notification playerNotif = new Notification(
                            mp.getUser(),
                            NotificationType.PLAYER_JOINED,
                            "Nouveau joueur",
                            String.format("%s a rejoint le match '%s'", newPlayer.getFullName(), match.getTitle())
                    );
                    playerNotif.setRelatedEntityId(match.getId());
                    playerNotif.setRelatedEntityType("MATCH");
                    notificationRepository.save(playerNotif);
                });
    }

    /**
     * Notifie qu'un joueur a quitté le match
     */
    @Async
    public void notifyPlayerLeft(Match match, User player) {
        logger.info("Notifying player left: {} from match {}", player.getFullName(), match.getTitle());

        // Notifier le créateur
        if (!match.getCreator().getId().equals(player.getId())) {
            Notification notification = new Notification(
                    match.getCreator(),
                    NotificationType.PLAYER_LEFT,
                    "Un joueur a quitté votre match",
                    String.format("%s a quitté le match '%s'", player.getFullName(), match.getTitle())
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notificationRepository.save(notification);
        }
    }

    /**
     * Notifie que le match est confirmé
     */
    @Async
    public void notifyMatchConfirmed(Match match) {
        logger.info("Notifying match confirmed: {}", match.getTitle());

        match.getPlayers().forEach(mp -> {
            Notification notification = new Notification(
                    mp.getUser(),
                    NotificationType.MATCH_CONFIRMED,
                    "Match confirmé !",
                    String.format("Le match '%s' est confirmé et aura lieu le %s",
                            match.getTitle(),
                            match.getSlot().getStartTime())
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notification.setActionUrl("/matches/" + match.getId());
            notificationRepository.save(notification);

            // Envoyer aussi un email
            emailService.sendMatchConfirmedEmail(mp.getUser(), match);
        });
    }

    /**
     * Notifie que le match va commencer
     */
    @Async
    public void notifyMatchStarting(Match match) {
        logger.info("Notifying match starting: {}", match.getTitle());

        match.getPlayers().forEach(mp -> {
            Notification notification = new Notification(
                    mp.getUser(),
                    NotificationType.MATCH_STARTING_SOON,
                    "Le match commence !",
                    String.format("Le match '%s' commence maintenant au %s",
                            match.getTitle(),
                            match.getField().getName())
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notificationRepository.save(notification);
        });
    }

    /**
     * Notifie que le match est terminé avec les résultats
     */
    @Async
    public void notifyMatchCompleted(Match match) {
        logger.info("Notifying match completed: {}", match.getTitle());

        String resultMessage = String.format(
                "Match terminé ! Score final : Équipe A %d - %d Équipe B",
                match.getScoreTeamA(),
                match.getScoreTeamB()
        );

        if ("DRAW".equals(match.getWinnerTeam())) {
            resultMessage += " (Match nul)";
        } else {
            resultMessage += String.format(" (Victoire %s)", match.getWinnerTeam());
        }

        String finalResultMessage = resultMessage;
        match.getPlayers().forEach(mp -> {
            Notification notification = new Notification(
                    mp.getUser(),
                    NotificationType.MATCH_COMPLETED,
                    "Match terminé",
                    finalResultMessage
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notificationRepository.save(notification);
        });
    }

    /**
     * Notifie l'annulation d'un match
     */
    @Async
    public void notifyMatchCancelled(Match match, String reason) {
        logger.info("Notifying match cancelled: {}", match.getTitle());

        match.getPlayers().forEach(mp -> {
            Notification notification = new Notification(
                    mp.getUser(),
                    NotificationType.MATCH_CANCELLED,
                    "Match annulé",
                    String.format("Le match '%s' a été annulé. Raison : %s",
                            match.getTitle(),
                            reason != null ? reason : "Non spécifiée")
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notificationRepository.save(notification);

            // Envoyer un email
            emailService.sendMatchCancelledEmail(mp.getUser(), match, reason);
        });
    }

    /**
     * Envoie un rappel pour les matchs à venir
     */
    @Scheduled(cron = "0 0 * * * *") // Toutes les heures
    @Transactional
    public void sendMatchReminders() {
        logger.info("Sending match reminders");

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime reminderTimeStart = now.plusHours(2); // Rappel 2h avant
        LocalDateTime reminderTimeEnd = now.plusHours(3);

        // Rechercher les matchs qui commencent dans 2-3h
        List<Match> upcomingMatches = findMatchesInTimeRange(reminderTimeStart, reminderTimeEnd);

        for (Match match : upcomingMatches) {
            sendMatchReminderNotifications(match);
        }

        logger.info("Match reminders sent for {} matches", upcomingMatches.size());
    }

    /**
     * Récupère les notifications non lues d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Notification> getUnreadNotifications(UUID userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    /**
     * Récupère toutes les notifications d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(UUID userId, int limit) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .limit(limit)
                .collect(Collectors.toList());
    }

    /**
     * Marque une notification comme lue
     */
    public void markAsRead(UUID notificationId) {
        notificationRepository.findById(notificationId).ifPresent(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Marque toutes les notifications d'un utilisateur comme lues
     */
    public void markAllAsRead(UUID userId) {
        List<Notification> unreadNotifications = getUnreadNotifications(userId);
        unreadNotifications.forEach(notification -> {
            notification.markAsRead();
            notificationRepository.save(notification);
        });
    }

    /**
     * Supprime les notifications expirées
     */
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupExpiredNotifications() {
        logger.info("Cleaning up expired notifications");

        LocalDateTime now = LocalDateTime.now();
        List<Notification> expiredNotifications = notificationRepository.findAll()
                .stream()
                .filter(Notification::isExpired)
                .collect(Collectors.toList());

        expiredNotifications.forEach(notificationRepository::delete);

        logger.info("Deleted {} expired notifications", expiredNotifications.size());
    }

    /**
     * Notifie les invitations d'équipe
     */
    @Async
    public void notifyTeamInvitation(User user, Team team) {
        logger.info("Notifying team invitation: {} to user {}", team.getName(), user.getFullName());

        Notification notification = new Notification(
                user,
                NotificationType.TEAM_INVITATION,
                "Invitation d'équipe",
                String.format("Vous êtes invité(e) à rejoindre l'équipe '%s'", team.getName())
        );
        notification.setRelatedEntityId(team.getId());
        notification.setRelatedEntityType("TEAM");
        notification.setActionUrl("/teams/" + team.getId() + "/join");
        notification.setExpiresAt(LocalDateTime.now().plusDays(7)); // Expire dans 7 jours

        notificationRepository.save(notification);

        // Envoyer aussi un email
        emailService.sendTeamInvitationEmail(user, team);
    }

    /**
     * Notifie les opportunités de matchmaking
     */
    @Async
    public void notifyMatchingOpportunity(User user, Match match, double compatibilityScore) {
        logger.info("Notifying matching opportunity: match {} to user {} (score: {})", 
                   match.getTitle(), user.getFullName(), compatibilityScore);

        if (compatibilityScore < 7.0) {
            return; // Ne notifier que les matchs très compatibles
        }

        Notification notification = new Notification(
                user,
                NotificationType.MATCH_SUGGESTION,
                "Match parfait trouvé !",
                String.format("Un match '%s' correspond parfaitement à vos préférences (compatibilité: %.1f/10)", 
                        match.getTitle(), compatibilityScore)
        );
        notification.setRelatedEntityId(match.getId());
        notification.setRelatedEntityType("MATCH");
        notification.setActionUrl("/matches/" + match.getId() + "/join");
        notification.setExpiresAt(match.getRegistrationDeadline());

        notificationRepository.save(notification);
    }

    /**
     * Notifications push en temps réel
     */
    @Async
    public void sendRealTimeNotification(User user, NotificationType type, String title, String message, String actionUrl) {
        logger.info("Sending real-time notification to user {}: {}", user.getId(), title);

        Notification notification = new Notification(user, type, title, message);
        notification.setActionUrl(actionUrl);
        notificationRepository.save(notification);

        // TODO: Implémenter WebSocket ou Server-Sent Events pour les notifications temps réel
        // webSocketService.sendNotification(user.getId(), notification);
    }

    /**
     * Notifications groupées intelligentes
     */
    @Scheduled(cron = "0 0 9 * * *") // Tous les jours à 9h
    @Transactional
    public void sendDailyDigest() {
        logger.info("Sending daily digest notifications");

        List<User> activeUsers = getActiveUsers();

        for (User user : activeUsers) {
            List<Notification> unreadNotifications = getUnreadNotifications(user.getId());
            
            if (!unreadNotifications.isEmpty()) {
                sendDailyDigestNotification(user, unreadNotifications);
            }
        }

        logger.info("Daily digest sent to {} users", activeUsers.size());
    }

    // Méthodes privées d'aide

    private List<Match> findMatchesInTimeRange(LocalDateTime start, LocalDateTime end) {
        // Cette méthode devrait être dans MatchRepository, mais pour simplifier ici
        return List.of(); // TODO: Implémenter la recherche réelle
    }

    private void sendMatchReminderNotifications(Match match) {
        match.getPlayers().forEach(mp -> {
            Notification notification = new Notification(
                    mp.getUser(),
                    NotificationType.MATCH_REMINDER,
                    "Rappel de match",
                    String.format("N'oubliez pas votre match '%s' qui commence dans 2 heures au %s", 
                            match.getTitle(), match.getField().getName())
            );
            notification.setRelatedEntityId(match.getId());
            notification.setRelatedEntityType("MATCH");
            notification.setActionUrl("/matches/" + match.getId());
            notificationRepository.save(notification);

            // Envoyer email de rappel
            emailService.sendMatchReminderEmail(mp.getUser(), match);
        });
    }

    private List<User> getActiveUsers() {
        // Retourner les utilisateurs actifs (connectés récemment)
        return List.of(); // TODO: Implémenter la logique réelle
    }

    private void sendDailyDigestNotification(User user, List<Notification> notifications) {
        StringBuilder digestContent = new StringBuilder();
        digestContent.append(String.format("Bonjour %s,\n\n", user.getFirstName()));
        digestContent.append("Voici un résumé de vos notifications :\n\n");

        Map<NotificationType, Long> notificationCounts = notifications.stream()
                .collect(Collectors.groupingBy(Notification::getType, Collectors.counting()));

        notificationCounts.forEach((type, count) -> {
            digestContent.append(String.format("- %s: %d nouvelle(s) notification(s)\n", 
                    getNotificationTypeLabel(type), count));
        });

        digestContent.append("\nConnectez-vous pour voir tous les détails.");

        emailService.sendDailyDigestEmail(user, digestContent.toString());
    }

    private String getNotificationTypeLabel(NotificationType type) {
        switch (type) {
            case MATCH_CONFIRMATION_REQUEST: return "Confirmations de match";
            case PLAYER_JOINED: return "Nouveaux joueurs";
            case MATCH_SUGGESTION: return "Suggestions de match";
            case TEAM_INVITATION: return "Invitations d'équipe";
            default: return type.name();
        }
    }
}

// Interface pour EmailService (à implémenter séparément)
interface EmailService {
    void sendMatchCreatedEmail(User user, Match match);
    void sendMatchConfirmedEmail(User user, Match match);
    void sendMatchCancelledEmail(User user, Match match, String reason);
    void sendMatchReminderEmail(User user, Match match);
    void sendTeamInvitationEmail(User user, Team team);
    void sendDailyDigestEmail(User user, String digestContent);
}

// Implémentation basique d'EmailService
@Service
class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    @Override
    public void sendMatchCreatedEmail(User user, Match match) {
        logger.info("Sending match created email to {}", user.getEmail());
        // TODO: Implémenter l'envoi d'email réel
    }

    @Override
    public void sendMatchConfirmedEmail(User user, Match match) {
        logger.info("Sending match confirmed email to {}", user.getEmail());
        // TODO: Implémenter l'envoi d'email réel
    }

    @Override
    public void sendMatchCancelledEmail(User user, Match match, String reason) {
        logger.info("Sending match cancelled email to {}", user.getEmail());
        // TODO: Implémenter l'envoi d'email réel
    }

    @Override
    public void sendMatchReminderEmail(User user, Match match) {
        logger.info("Sending match reminder email to {}", user.getEmail());
        // TODO: Implémenter l'envoi d'email réel
    }

    @Override
    public void sendTeamInvitationEmail(User user, Team team) {
        logger.info("Sending team invitation email to {} for team {}", user.getEmail(), team.getName());
        // TODO: Implémenter l'envoi d'email réel
    }

    @Override
    public void sendDailyDigestEmail(User user, String digestContent) {
        logger.info("Sending daily digest email to {}", user.getEmail());
        // TODO: Implémenter l'envoi d'email réel
    }
}