package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.*;
import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;
import com.footArena.booking.domain.repositories.MatchRepository;
import com.footArena.booking.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchmakingService {

    private static final Logger logger = LoggerFactory.getLogger(MatchmakingService.class);

    private final MatchRepository matchRepository;
    private final UserRepository userRepository;

    public MatchmakingService(MatchRepository matchRepository, UserRepository userRepository) {
        this.matchRepository = matchRepository;
        this.userRepository = userRepository;
    }

    /**
     * Algorithme principal de matchmaking intelligent
     */
    @Transactional(readOnly = true)
    public Match findBestMatch(UUID userId, MatchType preferredType, SkillLevel skillLevel, String location) {
        logger.info("Finding best match for user {} - type: {} level: {} location: {}", 
                   userId, preferredType, skillLevel, location);

        User user = getUserById(userId);
        List<Match> availableMatches = getAvailableMatches(user, preferredType);

        if (availableMatches.isEmpty()) {
            logger.info("No available matches found");
            return null;
        }

        // Calculer le score de compatibilité pour chaque match
        List<MatchScore> scoredMatches = availableMatches.stream()
                .map(match -> new MatchScore(match, calculateCompatibilityScore(user, match, preferredType, skillLevel, location)))
                .sorted((a, b) -> Double.compare(b.score, a.score)) // Tri décroissant
                .collect(Collectors.toList());

        logger.info("Found {} potential matches, best score: {}", 
                   scoredMatches.size(), scoredMatches.get(0).score);

        return scoredMatches.get(0).match;
    }

    /**
     * Équilibrage intelligent des équipes basé sur les niveaux de compétence
     */
    public String assignPlayerToBalancedTeam(Match match, User newPlayer) {
        logger.debug("Balancing teams for match {} with new player {}", match.getId(), newPlayer.getId());

        List<MatchPlayer> teamAPlayers = getTeamPlayers(match, "TEAM_A");
        List<MatchPlayer> teamBPlayers = getTeamPlayers(match, "TEAM_B");

        // Si une équipe a moins de joueurs, l'assigner là
        if (teamAPlayers.size() < teamBPlayers.size() && match.hasSpaceInTeamA()) {
            return "TEAM_A";
        } else if (teamBPlayers.size() < teamAPlayers.size() && match.hasSpaceInTeamB()) {
            return "TEAM_B";
        }

        // Si les équipes ont le même nombre, équilibrer par niveau de compétence
        double teamAAvgSkill = calculateTeamAverageSkill(teamAPlayers);
        double teamBAvgSkill = calculateTeamAverageSkill(teamBPlayers);
        double newPlayerSkill = getPlayerSkillLevel(newPlayer);

        // Calculer l'impact sur l'équilibre pour chaque équipe
        double teamABalanceAfter = Math.abs((teamAAvgSkill * teamAPlayers.size() + newPlayerSkill) / (teamAPlayers.size() + 1) - teamBAvgSkill);
        double teamBBalanceAfter = Math.abs((teamBAvgSkill * teamBPlayers.size() + newPlayerSkill) / (teamBPlayers.size() + 1) - teamAAvgSkill);

        // Assigner à l'équipe qui aura le meilleur équilibre
        if (teamABalanceAfter < teamBBalanceAfter && match.hasSpaceInTeamA()) {
            return "TEAM_A";
        } else if (match.hasSpaceInTeamB()) {
            return "TEAM_B";
        } else if (match.hasSpaceInTeamA()) {
            return "TEAM_A";
        }

        return null; // Match complet
    }

    /**
     * Suggestions personnalisées de matchs basées sur l'historique du joueur
     */
    @Transactional(readOnly = true)
    public List<Match> getPersonalizedSuggestions(UUID userId, int maxResults) {
        logger.info("Getting personalized suggestions for user {}", userId);

        User user = getUserById(userId);
        UserProfile profile = analyzeUserProfile(user);

        List<Match> availableMatches = getAvailableMatches(user, null);

        return availableMatches.stream()
                .map(match -> new MatchScore(match, calculatePersonalizedScore(profile, match)))
                .sorted((a, b) -> Double.compare(b.score, a.score))
                .limit(maxResults)
                .map(ms -> ms.match)
                .collect(Collectors.toList());
    }

    /**
     * Notification proactive de matchs correspondant aux préférences
     */
    public void notifyMatchingOpportunities(UUID userId) {
        logger.info("Checking matching opportunities for user {}", userId);

        User user = getUserById(userId);
        UserProfile profile = analyzeUserProfile(user);

        List<Match> recentMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getCreatedAt().isAfter(LocalDateTime.now().minusHours(1)))
                .filter(m -> calculatePersonalizedScore(profile, m) > 7.0) // Score élevé uniquement
                .collect(Collectors.toList());

        // TODO: Envoyer notifications push pour les matchs très compatibles
        recentMatches.forEach(match -> logger.info("High compatibility match found for user {}: {}", userId, match.getTitle()));
    }

    // Méthodes privées d'aide

    private List<Match> getAvailableMatches(User user, MatchType preferredType) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getIsPublic())
                .filter(m -> !m.isFull())
                .filter(m -> m.isRegistrationOpen())
                .filter(m -> !isUserInMatch(m, user.getId()))
                .filter(m -> preferredType == null || m.getMatchType() == preferredType)
                .collect(Collectors.toList());
    }

    private double calculateCompatibilityScore(User user, Match match, MatchType preferredType, SkillLevel skillLevel, String location) {
        double score = 0.0;

        // Score basé sur le type de match préféré (30%)
        if (preferredType != null && match.getMatchType() == preferredType) {
            score += 3.0;
        }

        // Score basé sur le niveau de compétence (25%)
        if (skillLevel != null) {
            int levelDifference = Math.abs(skillLevel.ordinal() - match.getSkillLevel().ordinal());
            score += (3 - levelDifference) * 0.833; // Max 2.5 points
        }

        // Score basé sur la proximité temporelle (20%)
        long hoursUntilMatch = java.time.Duration.between(LocalDateTime.now(), match.getSlot().getStartTime()).toHours();
        if (hoursUntilMatch <= 24) {
            score += 2.0 * (1 - hoursUntilMatch / 24.0); // Plus proche = meilleur score
        }

        // Score basé sur le taux de remplissage (15%)
        double fillRatio = (double) match.getTotalPlayers() / (match.getMaxPlayersPerTeam() * 2);
        if (fillRatio > 0.5) { // Match déjà bien rempli
            score += 1.5 * fillRatio;
        }

        // Score basé sur la localisation (10%) - pour l'instant simpliste
        if (location != null && match.getField().getEstablishment().getAddress().contains(location)) {
            score += 1.0;
        }

        return Math.min(score, 10.0); // Score max 10
    }

    private double calculatePersonalizedScore(UserProfile profile, Match match) {
        double score = 0.0;

        // Score basé sur les types de matchs préférés du joueur
        if (profile.preferredMatchTypes.contains(match.getMatchType())) {
            score += 3.0;
        }

        // Score basé sur les créneaux préférés
        int matchHour = match.getSlot().getStartTime().getHour();
        if (profile.preferredTimeSlots.contains(matchHour)) {
            score += 2.0;
        }

        // Score basé sur le niveau de compétence habituel
        int skillDifference = Math.abs(profile.averageSkillLevel.ordinal() - match.getSkillLevel().ordinal());
        score += (3 - skillDifference) * 0.833;

        // Score basé sur la régularité de jeu
        if (profile.averageMatchesPerWeek > 2 && match.getSlot().getStartTime().isAfter(LocalDateTime.now().plusDays(1))) {
            score += 1.0; // Joueur régulier, match dans le futur
        }

        return Math.min(score, 10.0);
    }

    private UserProfile analyzeUserProfile(User user) {
        UserProfile profile = new UserProfile();

        // Analyser l'historique des matchs de l'utilisateur
        List<Match> userMatches = matchRepository.findAll().stream()
                .filter(m -> isUserInMatch(m, user.getId()))
                .filter(m -> m.getCompletedAt() != null)
                .filter(m -> m.getCompletedAt().isAfter(LocalDateTime.now().minusMonths(3))) // 3 derniers mois
                .collect(Collectors.toList());

        if (!userMatches.isEmpty()) {
            // Types de matchs préférés
            Map<MatchType, Long> typeCount = userMatches.stream()
                    .collect(Collectors.groupingBy(Match::getMatchType, Collectors.counting()));
            profile.preferredMatchTypes = typeCount.entrySet().stream()
                    .filter(entry -> entry.getValue() > userMatches.size() * 0.3) // Au moins 30%
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // Créneaux préférés
            Map<Integer, Long> timeSlots = userMatches.stream()
                    .collect(Collectors.groupingBy(m -> m.getSlot().getStartTime().getHour(), Collectors.counting()));
            profile.preferredTimeSlots = timeSlots.entrySet().stream()
                    .filter(entry -> entry.getValue() > userMatches.size() * 0.2) // Au moins 20%
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toSet());

            // Niveau moyen
            double avgSkill = userMatches.stream()
                    .mapToInt(m -> m.getSkillLevel().ordinal())
                    .average()
                    .orElse(SkillLevel.INTERMEDIATE.ordinal());
            profile.averageSkillLevel = SkillLevel.values()[(int) Math.round(avgSkill)];

            // Fréquence de jeu
            long weeksSinceFirstMatch = java.time.temporal.ChronoUnit.WEEKS.between(
                    userMatches.stream().map(Match::getCompletedAt).min(LocalDateTime::compareTo).orElse(LocalDateTime.now()),
                    LocalDateTime.now()
            );
            profile.averageMatchesPerWeek = weeksSinceFirstMatch > 0 ? (double) userMatches.size() / weeksSinceFirstMatch : 0;
        }

        return profile;
    }

    private boolean isUserInMatch(Match match, UUID userId) {
        return match.getPlayers().stream()
                .anyMatch(mp -> mp.getUser().getId().equals(userId));
    }

    private List<MatchPlayer> getTeamPlayers(Match match, String team) {
        return match.getPlayers().stream()
                .filter(mp -> team.equals(mp.getTeam()))
                .collect(Collectors.toList());
    }

    private double calculateTeamAverageSkill(List<MatchPlayer> teamPlayers) {
        return teamPlayers.stream()
                .mapToDouble(mp -> getPlayerSkillLevel(mp.getUser()))
                .average()
                .orElse(5.0); // Valeur par défaut
    }

    private double getPlayerSkillLevel(User user) {
        // Pour l'instant, utiliser le niveau général du user ou une valeur par défaut
        // TODO: Implémenter un système de rating plus sophistiqué
        return 5.0; // Niveau intermédiaire
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    // Classes internes pour l'algorithme

    private static class MatchScore {
        final Match match;
        final double score;

        MatchScore(Match match, double score) {
            this.match = match;
            this.score = score;
        }
    }

    private static class UserProfile {
        Set<MatchType> preferredMatchTypes = new HashSet<>();
        Set<Integer> preferredTimeSlots = new HashSet<>();
        SkillLevel averageSkillLevel = SkillLevel.INTERMEDIATE;
        double averageMatchesPerWeek = 0;
    }
}