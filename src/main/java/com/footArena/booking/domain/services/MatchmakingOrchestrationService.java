package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.*;
import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.NotificationType;
import com.footArena.booking.domain.enums.SkillLevel;
import com.footArena.booking.domain.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service d'orchestration pour la logique métier complexe du matchmaking
 * Coordonne tous les autres services pour créer une expérience intelligente
 */
// @Service // Temporairement désactivé
@Transactional
public class MatchmakingOrchestrationService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingOrchestrationService.class);

    private final MatchService matchService;
    private final MatchmakingService matchmakingService;
    private final TeamManagementService teamManagementService;
    private final NotificationService notificationService;
    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MatchmakingOrchestrationService(MatchService matchService,
                                         MatchmakingService matchmakingService,
                                         // TeamManagementService teamManagementService,
                                         NotificationService notificationService,
                                         MatchRepository matchRepository,
                                         UserRepository userRepository) {
        this.matchService = matchService;
        this.matchmakingService = matchmakingService;
        // this.teamManagementService = teamManagementService;
        this.teamManagementService = null; // Temporaire
        this.notificationService = notificationService;
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    /**
     * Processus complet de matchmaking intelligent pour un utilisateur
     */
    @Async
    public void performIntelligentMatchmaking(UUID userId) {
        logger.info("Starting intelligent matchmaking for user {}", userId);

        try {
            User user = getUserById(userId);
            
            // 1. Analyser le profil utilisateur
            UserMatchingProfile profile = analyzeUserProfile(user);
            
            // 2. Rechercher les meilleurs matchs
            List<Match> suggestedMatches = matchmakingService.getPersonalizedSuggestions(userId, 5);
            
            // 3. Notifier les opportunités
            for (Match match : suggestedMatches) {
                double compatibility = calculateMatchCompatibility(profile, match);
                if (compatibility > 7.0) {
                    notificationService.notifyMatchingOpportunity(user, match, compatibility);
                }
            }
            
            // 4. Vérifier si l'utilisateur peut créer un match
            if (shouldSuggestMatchCreation(profile)) {
                suggestOptimalMatchCreation(user, profile);
            }
            
            logger.info("Intelligent matchmaking completed for user {}", userId);
            
        } catch (Exception e) {
            logger.error("Error in intelligent matchmaking for user {}: {}", userId, e.getMessage());
        }
    }

    /**
     * Formation automatique d'équipes pour tous les matchs en attente
     */
    @Scheduled(cron = "0 */15 * * * *") // Toutes les 15 minutes
    @Transactional
    public void autoFormTeamsForPendingMatches() {
        logger.info("Starting automatic team formation for pending matches");

        List<Match> pendingMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getTotalPlayers() >= m.getMinPlayersToStart())
                .filter(m -> hasUnbalancedTeams(m))
                .collect(Collectors.toList());

        int balancedCount = 0;
        for (Match match : pendingMatches) {
            try {
                balanceMatchTeams(match);
                balancedCount++;
            } catch (Exception e) {
                logger.error("Error balancing teams for match {}: {}", match.getId(), e.getMessage());
            }
        }

        logger.info("Balanced teams for {}/{} pending matches", balancedCount, pendingMatches.size());
    }

    /**
     * Optimisation proactive des matchs (confirmation automatique, suggestions, etc.)
     */
    @Scheduled(cron = "0 */30 * * * *") // Toutes les 30 minutes
    @Transactional
    public void optimizeMatchExperience() {
        logger.info("Starting match experience optimization");

        // 1. Confirmer automatiquement les matchs prêts
        autoConfirmReadyMatches();
        
        // 2. Rechercher des joueurs pour les matchs incomplets
        findPlayersForIncompleteMatches();
        
        // 3. Suggérer des créneaux alternatifs pour les matchs qui peinent à se remplir
        suggestAlternativeSlots();
        
        logger.info("Match experience optimization completed");
    }

    /**
     * Système de réputation et ajustement des niveaux de compétence
     */
    @Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h du matin
    @Transactional
    public void updatePlayerReputationAndSkills() {
        logger.info("Starting player reputation and skill updates");

        List<User> activeUsers = getActiveUsers();
        
        for (User user : activeUsers) {
            try {
                PlayerStats stats = calculatePlayerStats(user);
                updateUserSkillLevel(user, stats);
                updatePlayerReputation(user, stats);
            } catch (Exception e) {
                logger.error("Error updating stats for user {}: {}", user.getId(), e.getMessage());
            }
        }

        logger.info("Updated reputation and skills for {} players", activeUsers.size());
    }

    /**
     * Processus de match-making basé sur les préférences d'équipe
     */
    public MatchResult createOptimizedMatch(UUID userId, MatchType matchType, UUID preferredTeamId) {
        logger.info("Creating optimized match for user {} with team {}", userId, preferredTeamId);

        User user = getUserById(userId);
        
        // 1. Analyser l'équipe préférée si fournie
        Team preferredTeam = null;
        if (preferredTeamId != null) {
            preferredTeam = getTeamById(preferredTeamId);
        }
        
        // 2. Rechercher les créneaux optimaux
        List<SlotRecommendation> recommendedSlots = findOptimalTimeSlots(user, matchType);
        
        if (recommendedSlots.isEmpty()) {
            return MatchResult.noSlotsAvailable();
        }
        
        SlotRecommendation bestSlot = recommendedSlots.get(0);
        
        // 3. Créer le match
        Match match = matchService.createMatch(
                userId,
                bestSlot.slot.getField().getId(),
                bestSlot.slot.getId(),
                generateMatchTitle(matchType, bestSlot),
                "Match créé automatiquement par le système intelligent",
                matchType,
                user.getSkillLevel() != null ? user.getSkillLevel() : SkillLevel.INTERMEDIATE,
                true,
                null
        );
        
        // 4. Inviter l'équipe préférée si applicable
        if (preferredTeam != null) {
            inviteTeamToMatch(preferredTeam, match);
        }
        
        // 5. Lancer le processus de recherche de joueurs
        initiatePlayerSearch(match);
        
        logger.info("Optimized match created: {} for user {}", match.getId(), userId);
        
        return MatchResult.success(match, bestSlot.expectedFillTime);
    }

    // Méthodes privées d'aide

    private UserMatchingProfile analyzeUserProfile(User user) {
        UserMatchingProfile profile = new UserMatchingProfile();
        profile.user = user;
        profile.skillLevel = user.getSkillLevel() != null ? user.getSkillLevel() : SkillLevel.INTERMEDIATE;
        
        // Analyser l'historique récent (3 derniers mois)
        List<Match> recentMatches = getRecentUserMatches(user.getId(), 90);
        
        if (!recentMatches.isEmpty()) {
            // Calculer les préférences
            profile.preferredMatchTypes = analyzePreferredMatchTypes(recentMatches);
            profile.preferredTimeSlots = analyzePreferredTimeSlots(recentMatches);
            profile.averageMatchesPerWeek = calculateAverageMatchesPerWeek(recentMatches);
            profile.preferredSkillLevels = analyzePreferredSkillLevels(recentMatches);
            
            // Calculer les métriques de performance
            profile.completionRate = calculateCompletionRate(recentMatches);
            profile.averageRating = calculateAverageRating(user);
        }
        
        return profile;
    }

    private double calculateMatchCompatibility(UserMatchingProfile profile, Match match) {
        double compatibility = 0.0;
        
        // Compatibilité de type (30%)
        if (profile.preferredMatchTypes.contains(match.getMatchType())) {
            compatibility += 3.0;
        }
        
        // Compatibilité de niveau (25%)
        int levelDiff = Math.abs(profile.skillLevel.ordinal() - match.getSkillLevel().ordinal());
        compatibility += Math.max(0, 2.5 - (levelDiff * 0.5));
        
        // Compatibilité temporelle (20%)
        int matchHour = match.getSlot().getStartTime().getHour();
        if (profile.preferredTimeSlots.contains(matchHour)) {
            compatibility += 2.0;
        }
        
        // Taux de remplissage optimal (15%)
        double fillRatio = (double) match.getTotalPlayers() / (match.getMaxPlayersPerTeam() * 2);
        if (fillRatio >= 0.3 && fillRatio <= 0.8) { // Ni trop vide ni trop plein
            compatibility += 1.5;
        }
        
        // Distance temporelle (10%)
        long hoursUntilMatch = java.time.Duration.between(LocalDateTime.now(), match.getSlot().getStartTime()).toHours();
        if (hoursUntilMatch >= 2 && hoursUntilMatch <= 72) { // Entre 2h et 3 jours
            compatibility += 1.0;
        }
        
        return Math.min(compatibility, 10.0);
    }

    private boolean shouldSuggestMatchCreation(UserMatchingProfile profile) {
        return profile.averageMatchesPerWeek >= 1.0 && 
               profile.completionRate >= 0.7 &&
               profile.user.isEnabled();
    }

    private void suggestOptimalMatchCreation(User user, UserMatchingProfile profile) {
        List<SlotRecommendation> recommendations = findOptimalTimeSlots(user, 
                profile.preferredMatchTypes.isEmpty() ? MatchType.FIVE_VS_FIVE : 
                profile.preferredMatchTypes.iterator().next());
        
        if (!recommendations.isEmpty()) {
            SlotRecommendation best = recommendations.get(0);
            notificationService.sendRealTimeNotification(
                    user,
                    NotificationType.MATCH_SUGGESTION,
                    "Créneaux optimaux disponibles",
                    String.format("Nous avons trouvé des créneaux parfaits pour vous au %s. Probabilité de match complet: %.0f%%",
                            best.slot.getField().getName(), best.successProbability * 100),
                    "/matches/create?slotId=" + best.slot.getId()
            );
        }
    }

    private boolean hasUnbalancedTeams(Match match) {
        List<MatchPlayer> teamA = match.getPlayers().stream()
                .filter(mp -> "TEAM_A".equals(mp.getTeam()))
                .collect(Collectors.toList());
        
        List<MatchPlayer> teamB = match.getPlayers().stream()
                .filter(mp -> "TEAM_B".equals(mp.getTeam()))
                .collect(Collectors.toList());
        
        return Math.abs(teamA.size() - teamB.size()) > 1;
    }

    private void balanceMatchTeams(Match match) {
        // List<MatchPlayer> allPlayers = new ArrayList<>(match.getPlayers());
        // TeamManagementService.TeamFormationResult result = 
        //         teamManagementService.formBalancedTeams(allPlayers, match);
        
        logger.info("Rebalanced teams for match {} (team management temporarily disabled)", 
                   match.getId());
    }

    private void autoConfirmReadyMatches() {
        List<Match> readyMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getTotalPlayers() >= m.getMinPlayersToStart())
                .filter(m -> m.getSlot().getStartTime().isAfter(LocalDateTime.now().plusHours(1))) // Au moins 1h à l'avance
                .collect(Collectors.toList());
        
        for (Match match : readyMatches) {
            matchService.confirmMatch(match.getId());
            logger.info("Auto-confirmed match: {}", match.getId());
        }
    }

    private void findPlayersForIncompleteMatches() {
        List<Match> incompleteMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getTotalPlayers() < m.getMinPlayersToStart())
                .filter(m -> m.getSlot().getStartTime().isAfter(LocalDateTime.now().plusHours(2)))
                .collect(Collectors.toList());
        
        for (Match match : incompleteMatches) {
            List<User> suggestedPlayers = findCompatiblePlayers(match);
            suggestedPlayers.stream().limit(3).forEach(user -> {
                notificationService.notifyMatchingOpportunity(user, match, 8.0);
            });
        }
    }

    private void suggestAlternativeSlots() {
        // TODO: Implémenter la suggestion de créneaux alternatifs
        logger.info("Alternative slot suggestions - feature coming soon");
    }

    private List<User> findCompatiblePlayers(Match match) {
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(user -> !isUserInMatch(match, user.getId()))
                .limit(10)
                .collect(Collectors.toList());
    }

    private PlayerStats calculatePlayerStats(User user) {
        PlayerStats stats = new PlayerStats();
        List<Match> userMatches = getRecentUserMatches(user.getId(), 30); // 30 derniers jours
        
        stats.totalMatches = userMatches.size();
        stats.completedMatches = (int) userMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .count();
        stats.completionRate = stats.totalMatches > 0 ? 
                (double) stats.completedMatches / stats.totalMatches : 0.0;
        
        return stats;
    }

    private void updateUserSkillLevel(User user, PlayerStats stats) {
        // Logique d'ajustement du niveau basée sur les performances
        if (stats.completionRate > 0.9 && stats.totalMatches > 10) {
            // Possibilité d'augmenter le niveau
            SkillLevel currentLevel = user.getSkillLevel() != null ? user.getSkillLevel() : SkillLevel.INTERMEDIATE;
            // TODO: Implémenter la logique d'ajustement du niveau
        }
    }

    private void updatePlayerReputation(User user, PlayerStats stats) {
        // TODO: Implémenter le système de réputation
        logger.debug("Updated reputation for user {} - completion rate: {}", 
                    user.getId(), stats.completionRate);
    }

    // Classes internes et méthodes utilitaires (simplifiées pour l'exemple)

    private static class UserMatchingProfile {
        User user;
        SkillLevel skillLevel;
        Set<MatchType> preferredMatchTypes = new HashSet<>();
        Set<Integer> preferredTimeSlots = new HashSet<>();
        Set<SkillLevel> preferredSkillLevels = new HashSet<>();
        double averageMatchesPerWeek;
        double completionRate;
        double averageRating;
    }

    private static class SlotRecommendation {
        Slot slot;
        double successProbability;
        int expectedFillTime; // minutes
        
        SlotRecommendation(Slot slot, double probability, int fillTime) {
            this.slot = slot;
            this.successProbability = probability;
            this.expectedFillTime = fillTime;
        }
    }

    public static class MatchResult {
        public final boolean success;
        public final Match match;
        public final String message;
        public final int expectedFillTimeMinutes;
        
        private MatchResult(boolean success, Match match, String message, int fillTime) {
            this.success = success;
            this.match = match;
            this.message = message;
            this.expectedFillTimeMinutes = fillTime;
        }
        
        public static MatchResult success(Match match, int fillTime) {
            return new MatchResult(true, match, "Match créé avec succès", fillTime);
        }
        
        public static MatchResult noSlotsAvailable() {
            return new MatchResult(false, null, "Aucun créneau optimal disponible", 0);
        }
    }

    private static class PlayerStats {
        int totalMatches;
        int completedMatches;
        double completionRate;
    }

    // Méthodes utilitaires simplifiées (à implémenter complètement)

    private List<Match> getRecentUserMatches(UUID userId, int days) {
        return matchRepository.findAll().stream()
                .filter(m -> isUserInMatch(m, userId))
                .filter(m -> m.getCreatedAt().isAfter(LocalDateTime.now().minusDays(days)))
                .collect(Collectors.toList());
    }

    private boolean isUserInMatch(Match match, UUID userId) {
        return match.getPlayers().stream()
                .anyMatch(mp -> mp.getUser().getId().equals(userId));
    }

    private Set<MatchType> analyzePreferredMatchTypes(List<Match> matches) {
        return matches.stream()
                .collect(Collectors.groupingBy(Match::getMatchType, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= matches.size() * 0.3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<Integer> analyzePreferredTimeSlots(List<Match> matches) {
        return matches.stream()
                .map(m -> m.getSlot().getStartTime().getHour())
                .collect(Collectors.groupingBy(h -> h, Collectors.counting()))
                .entrySet().stream()
                .filter(entry -> entry.getValue() >= matches.size() * 0.2)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    private Set<SkillLevel> analyzePreferredSkillLevels(List<Match> matches) {
        return matches.stream()
                .map(Match::getSkillLevel)
                .collect(Collectors.toSet());
    }

    private double calculateAverageMatchesPerWeek(List<Match> matches) {
        if (matches.isEmpty()) return 0.0;
        
        LocalDateTime oldest = matches.stream()
                .map(Match::getCreatedAt)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        long weeks = java.time.temporal.ChronoUnit.WEEKS.between(oldest, LocalDateTime.now());
        return weeks > 0 ? (double) matches.size() / weeks : matches.size();
    }

    private double calculateCompletionRate(List<Match> matches) {
        long completed = matches.stream()
                .filter(m -> m.getStatus() == MatchStatus.COMPLETED)
                .count();
        return matches.isEmpty() ? 0.0 : (double) completed / matches.size();
    }

    private double calculateAverageRating(User user) {
        // TODO: Implémenter le système de rating
        return 5.0; // Valeur par défaut
    }

    private List<SlotRecommendation> findOptimalTimeSlots(User user, MatchType matchType) {
        // TODO: Implémenter la recherche de créneaux optimaux
        return List.of(); // Retour vide pour l'instant
    }

    private void inviteTeamToMatch(Team team, Match match) {
        // TODO: Implémenter l'invitation d'équipe
        logger.info("Inviting team {} to match {}", team.getName(), match.getId());
    }

    private void initiatePlayerSearch(Match match) {
        // TODO: Lancer la recherche automatique de joueurs
        logger.info("Initiating player search for match {}", match.getId());
    }

    private String generateMatchTitle(MatchType matchType, SlotRecommendation slot) {
        return String.format("Match %s - %s", 
                matchType.name().replace("_", " "),
                slot.slot.getStartTime().toLocalDate());
    }

    private List<User> getActiveUsers() {
        // TODO: Définir la logique des utilisateurs actifs
        return userRepository.findAll().stream()
                .filter(User::isEnabled)
                .limit(100)
                .collect(Collectors.toList());
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    private Team getTeamById(UUID teamId) {
        // TODO: Implémenter via TeamRepository
        throw new RuntimeException("Team repository not implemented");
    }
}