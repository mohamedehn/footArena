package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.*;
import com.footArena.booking.domain.enums.SkillLevel;
import com.footArena.booking.domain.repositories.TeamRepository;
import com.footArena.booking.domain.repositories.TeamMemberRepository;
import com.footArena.booking.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

// @Service // Temporairement désactivé
@Transactional
public class TeamManagementService {

    private static final Logger logger = LoggerFactory.getLogger(TeamManagementService.class);

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    public TeamManagementService(TeamRepository teamRepository, 
                               TeamMemberRepository teamMemberRepository,
                               UserRepository userRepository,
                               NotificationService notificationService) {
        this.teamRepository = teamRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
    }

    /**
     * Formation automatique d'équipes équilibrées pour un match
     */
    public TeamFormationResult formBalancedTeams(List<MatchPlayer> players, Match match) {
        logger.info("Forming balanced teams for match {} with {} players", match.getId(), players.size());

        if (players.size() < 2) {
            throw new IllegalArgumentException("At least 2 players needed to form teams");
        }

        // Analyser les niveaux de compétence des joueurs
        List<PlayerWithSkill> playersWithSkill = players.stream()
                .map(this::analyzePlayerSkill)
                .sorted((a, b) -> Double.compare(b.skillRating, a.skillRating)) // Trier par niveau décroissant
                .collect(Collectors.toList());

        // Algorithme de formation d'équipes équilibrées (Distribution alternée)
        List<PlayerWithSkill> teamA = new ArrayList<>();
        List<PlayerWithSkill> teamB = new ArrayList<>();
        boolean assignToTeamA = true;

        for (PlayerWithSkill playerSkill : playersWithSkill) {
            if (assignToTeamA) {
                teamA.add(playerSkill);
            } else {
                teamB.add(playerSkill);
            }
            assignToTeamA = !assignToTeamA;
        }

        // Ajustement fin pour équilibrer les niveaux
        optimizeTeamBalance(teamA, teamB);

        // Assigner les équipes aux joueurs
        teamA.forEach(ps -> ps.player.setTeam("TEAM_A"));
        teamB.forEach(ps -> ps.player.setTeam("TEAM_B"));

        double teamAAvg = teamA.stream().mapToDouble(ps -> ps.skillRating).average().orElse(0);
        double teamBAvg = teamB.stream().mapToDouble(ps -> ps.skillRating).average().orElse(0);

        logger.info("Teams formed - Team A avg skill: {}, Team B avg skill: {}, difference: {}", 
                   teamAAvg, teamBAvg, Math.abs(teamAAvg - teamBAvg));

        return new TeamFormationResult(teamA, teamB, Math.abs(teamAAvg - teamBAvg));
    }

    /**
     * Suggestion de joueurs manquants pour compléter une équipe
     */
    @Transactional(readOnly = true)
    public List<User> suggestPlayersForTeam(Match match, String teamName) {
        logger.info("Suggesting players for team {} in match {}", teamName, match.getId());

        // Analyser l'équipe actuelle
        List<MatchPlayer> currentTeam = match.getPlayers().stream()
                .filter(mp -> teamName.equals(mp.getTeam()))
                .collect(Collectors.toList());

        double currentTeamAvgSkill = currentTeam.stream()
                .mapToDouble(mp -> analyzePlayerSkill(mp).skillRating)
                .average()
                .orElse(5.0);

        // Analyser l'équipe adverse
        String oppositeTeam = "TEAM_A".equals(teamName) ? "TEAM_B" : "TEAM_A";
        List<MatchPlayer> oppositeTeamPlayers = match.getPlayers().stream()
                .filter(mp -> oppositeTeam.equals(mp.getTeam()))
                .collect(Collectors.toList());

        double oppositeTeamAvgSkill = oppositeTeamPlayers.stream()
                .mapToDouble(mp -> analyzePlayerSkill(mp).skillRating)
                .average()
                .orElse(5.0);

        // Trouver des joueurs disponibles avec un niveau approprié
        List<User> availableUsers = userRepository.findAll().stream()
                .filter(user -> !isUserInMatch(match, user.getId()))
                .filter(User::isEnabled)
                .collect(Collectors.toList());

        // Calculer le niveau idéal pour équilibrer
        double targetSkillLevel;
        if (currentTeam.size() > 0) {
            targetSkillLevel = (oppositeTeamAvgSkill * oppositeTeamPlayers.size() - currentTeamAvgSkill * currentTeam.size()) 
                             / (match.getMaxPlayersPerTeam() - currentTeam.size());
        } else {
            targetSkillLevel = oppositeTeamAvgSkill;
        }

        // Sélectionner les meilleurs candidats
        return availableUsers.stream()
                .map(user -> new UserWithCompatibility(user, calculateUserCompatibility(user, targetSkillLevel, match)))
                .sorted((a, b) -> Double.compare(b.compatibility, a.compatibility))
                .limit(10) // Top 10 suggestions
                .map(uwc -> uwc.user)
                .collect(Collectors.toList());
    }

    /**
     * Formation d'équipes automatique avec contraintes de position
     */
    public TeamFormationResult formTeamsWithPositions(List<MatchPlayer> players, Match match) {
        logger.info("Forming teams with position constraints for match {}", match.getId());

        Map<String, List<MatchPlayer>> playersByPosition = players.stream()
                .collect(Collectors.groupingBy(mp -> 
                    mp.getPositionPreference() != null ? mp.getPositionPreference() : "ANY"));

        // S'assurer qu'il y a au moins un gardien par équipe si possible
        List<MatchPlayer> goalkeepers = playersByPosition.getOrDefault("GK", new ArrayList<>());
        List<MatchPlayer> fieldPlayers = players.stream()
                .filter(mp -> !"GK".equals(mp.getPositionPreference()))
                .collect(Collectors.toList());

        TeamFormationResult result = formBalancedTeams(fieldPlayers, match);

        // Assigner les gardiens
        if (goalkeepers.size() >= 2) {
            goalkeepers.get(0).setTeam("TEAM_A");
            goalkeepers.get(1).setTeam("TEAM_B");
            
            // Ajouter les gardiens supplémentaires alternativement
            for (int i = 2; i < goalkeepers.size(); i++) {
                goalkeepers.get(i).setTeam(i % 2 == 0 ? "TEAM_A" : "TEAM_B");
            }
        } else if (goalkeepers.size() == 1) {
            // Assigner le gardien unique à l'équipe qui en a le plus besoin
            goalkeepers.get(0).setTeam(result.teamA.size() <= result.teamB.size() ? "TEAM_A" : "TEAM_B");
        }

        return result;
    }

    /**
     * Création d'une équipe permanente avec recherche de membres
     */
    public Team createTeamWithRecruitment(String name, UUID captainId, Integer maxPlayers, 
                                        SkillLevel targetSkillLevel, String description) {
        logger.info("Creating team {} with recruitment", name);

        User captain = getUserById(captainId);
        Team team = new Team(name, captain, maxPlayers);
        team.setDescription(description);
        team.setSkillLevel(targetSkillLevel);

        Team savedTeam = teamRepository.save(team);

        // Créer la relation capitaine-équipe
        TeamMember captainMember = new TeamMember();
        captainMember.setTeam(savedTeam);
        captainMember.setUser(captain);
        captainMember.setRole(TeamMember.TeamRole.CAPTAIN);
        captainMember.setStatus(TeamMember.MemberStatus.ACTIVE);
        teamMemberRepository.save(captainMember);

        // Lancer la recherche automatique de membres
        suggestAndInviteMembers(savedTeam);

        return savedTeam;
    }

    /**
     * Recherche et invitation automatique de membres pour une équipe
     */
    public void suggestAndInviteMembers(Team team) {
        logger.info("Suggesting members for team {}", team.getName());

        if (team.isFull()) {
            return;
        }

        // Critères de recherche basés sur l'équipe
        List<User> potentialMembers = userRepository.findAll().stream()
                .filter(User::isEnabled)
                .filter(user -> !isUserInTeam(team, user.getId()))
                .filter(user -> isUserCompatibleWithTeam(user, team))
                .limit(team.getAvailableSpots() * 3) // 3x plus de suggestions que de places
                .collect(Collectors.toList());

        // Envoyer des invitations aux meilleurs candidats
        potentialMembers.stream()
                .limit(team.getAvailableSpots())
                .forEach(user -> {
                    logger.info("Sending team invitation to user {} for team {}", user.getId(), team.getName());
                    // TODO: Implémenter système d'invitation
                    notificationService.notifyTeamInvitation(user, team);
                });
    }

    // Méthodes privées d'aide

    private PlayerWithSkill analyzePlayerSkill(MatchPlayer player) {
        // Analyser le niveau de compétence du joueur
        double skillRating = 5.0; // Valeur par défaut

        // Facteurs de calcul du niveau :
        // 1. Niveau déclaré du joueur
        if (player.getSkillLevel() != null) {
            skillRating = player.getSkillLevel().doubleValue();
        }

        // 2. Historique de performance (à implémenter)
        // 3. Position préférée
        if ("GK".equals(player.getPositionPreference())) {
            skillRating += 0.5; // Bonus pour les gardiens (rares)
        }

        return new PlayerWithSkill(player, skillRating);
    }

    private void optimizeTeamBalance(List<PlayerWithSkill> teamA, List<PlayerWithSkill> teamB) {
        // Algorithme d'optimisation simple : échange de joueurs pour équilibrer
        double teamATotal = teamA.stream().mapToDouble(ps -> ps.skillRating).sum();
        double teamBTotal = teamB.stream().mapToDouble(ps -> ps.skillRating).sum();

        if (Math.abs(teamATotal - teamBTotal) > 2.0 && teamA.size() > 1 && teamB.size() > 1) {
            // Tentative d'échange pour équilibrer
            PlayerWithSkill bestSwapA = null;
            PlayerWithSkill bestSwapB = null;
            double bestImprovement = 0;

            for (PlayerWithSkill playerA : teamA) {
                for (PlayerWithSkill playerB : teamB) {
                    double currentDiff = Math.abs(teamATotal - teamBTotal);
                    double newTeamATotal = teamATotal - playerA.skillRating + playerB.skillRating;
                    double newTeamBTotal = teamBTotal - playerB.skillRating + playerA.skillRating;
                    double newDiff = Math.abs(newTeamATotal - newTeamBTotal);
                    
                    if (newDiff < currentDiff) {
                        double improvement = currentDiff - newDiff;
                        if (improvement > bestImprovement) {
                            bestImprovement = improvement;
                            bestSwapA = playerA;
                            bestSwapB = playerB;
                        }
                    }
                }
            }

            // Effectuer l'échange optimal s'il améliore l'équilibre
            if (bestSwapA != null && bestSwapB != null) {
                teamA.remove(bestSwapA);
                teamB.remove(bestSwapB);
                teamA.add(bestSwapB);
                teamB.add(bestSwapA);
                
                logger.debug("Optimized team balance by swapping players");
            }
        }
    }

    private boolean isUserInMatch(Match match, UUID userId) {
        return match.getPlayers().stream()
                .anyMatch(mp -> mp.getUser().getId().equals(userId));
    }

    private boolean isUserInTeam(Team team, UUID userId) {
        return team.getMembers().stream()
                .anyMatch(tm -> tm.getUser().getId().equals(userId));
    }

    private boolean isUserCompatibleWithTeam(User user, Team team) {
        // Critères de compatibilité basique
        return true; // À améliorer avec plus de logique
    }

    private double calculateUserCompatibility(User user, double targetSkillLevel, Match match) {
        double compatibility = 10.0 - Math.abs(5.0 - targetSkillLevel); // Niveau de compétence
        
        // Ajouter d'autres facteurs de compatibilité
        // - Disponibilité historique
        // - Type de matchs préférés
        // - Localisation, etc.
        
        return Math.max(0, compatibility);
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found: " + userId));
    }

    // Classes internes

    public static class TeamFormationResult {
        public final List<PlayerWithSkill> teamA;
        public final List<PlayerWithSkill> teamB;
        public final double balanceDifference;

        public TeamFormationResult(List<PlayerWithSkill> teamA, List<PlayerWithSkill> teamB, double balanceDifference) {
            this.teamA = teamA;
            this.teamB = teamB;
            this.balanceDifference = balanceDifference;
        }
    }

    private static class PlayerWithSkill {
        final MatchPlayer player;
        final double skillRating;

        PlayerWithSkill(MatchPlayer player, double skillRating) {
            this.player = player;
            this.skillRating = skillRating;
        }
    }

    private static class UserWithCompatibility {
        final User user;
        final double compatibility;

        UserWithCompatibility(User user, double compatibility) {
            this.user = user;
            this.compatibility = compatibility;
        }
    }
}