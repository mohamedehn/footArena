package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.*;
import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.MatchPlayerRepository;
import com.footArena.booking.domain.repositories.MatchRepository;
import com.footArena.booking.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.annotation.Lazy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class MatchService {

    private static final Logger logger = LoggerFactory.getLogger(MatchService.class);

    private final MatchRepository matchRepository;
    private final MatchPlayerRepository matchPlayerRepository;
    private final UserRepository userRepository;
    private final FieldService fieldService;
    private final SlotService slotService;
    private final NotificationService notificationService;
    private final MatchmakingService matchmakingService;

    public MatchService(MatchRepository matchRepository,
                        MatchPlayerRepository matchPlayerRepository,
                        UserRepository userRepository,
                        FieldService fieldService,
                        SlotService slotService,
                        NotificationService notificationService,
                        @Lazy MatchmakingService matchmakingService) {
        this.matchRepository = matchRepository;
        this.matchPlayerRepository = matchPlayerRepository;
        this.userRepository = userRepository;
        this.fieldService = fieldService;
        this.slotService = slotService;
        this.notificationService = notificationService;
        this.matchmakingService = matchmakingService;
    }

    /**
     * Crée un nouveau match
     */
    public Match createMatch(UUID creatorId, UUID fieldId, UUID slotId, String title,
                             String description, MatchType matchType, SkillLevel skillLevel,
                             Boolean isPublic, BigDecimal entryFee) {

        logger.info("Creating match: {} by user: {}", title, creatorId);

        // Récupération des entités
        User creator = getUserById(creatorId);
        Field field = fieldService.getFieldById(fieldId);
        Slot slot = slotService.getSlotById(slotId);

        // Validations
        validateMatchCreation(field, slot, matchType);

        // Création du match
        Match match = new Match(field, slot, creator, title, matchType);
        match.setDescription(description);
        match.setSkillLevel(skillLevel != null ? skillLevel : SkillLevel.INTERMEDIATE);
        match.setIsPublic(isPublic != null ? isPublic : true);
        match.setEntryFee(entryFee);
        match.setAllowSubstitutes(true);
        match.setAutoStart(false);

        Match savedMatch = matchRepository.save(match);

        // Ajouter automatiquement le créateur comme premier joueur
        joinMatch(savedMatch.getId(), creatorId, null, null);

        // Notification
        notificationService.notifyMatchCreated(savedMatch);

        logger.info("Match created successfully with ID: {}", savedMatch.getId());
        return savedMatch;
    }

    /**
     * Rejoint un match
     */
    public MatchPlayer joinMatch(UUID matchId, UUID userId, String preferredTeam, String position) {
        logger.info("User {} joining match {}", userId, matchId);

        Match match = getMatchById(matchId);
        User user = getUserById(userId);

        // Validations
        validateJoinMatch(match, user);

        // Vérifier si le joueur n'est pas déjà inscrit
        boolean alreadyJoined = match.getPlayers().stream()
                .anyMatch(mp -> mp.getUser().getId().equals(userId));

        if (alreadyJoined) {
            throw new BusinessValidationException("User is already in this match");
        }

        // Déterminer l'équipe avec algorithme intelligent
        String assignedTeam = preferredTeam;
        if (assignedTeam == null || (!assignedTeam.equals("TEAM_A") && !assignedTeam.equals("TEAM_B"))) {
            assignedTeam = matchmakingService.assignPlayerToBalancedTeam(match, user);
        }

        if (assignedTeam == null) {
            throw new BusinessValidationException("Match is full");
        }

        // Vérifier que l'équipe choisie a de la place
        if (assignedTeam.equals("TEAM_A") && !match.hasSpaceInTeamA()) {
            assignedTeam = "TEAM_B";
        } else if (assignedTeam.equals("TEAM_B") && !match.hasSpaceInTeamB()) {
            assignedTeam = "TEAM_A";
        }

        // Créer l'inscription
        MatchPlayer matchPlayer = new MatchPlayer(match, user, assignedTeam);
        MatchPlayer savedPlayer = matchPlayerRepository.save(matchPlayer);

        // Mettre à jour les compteurs du match
        match.addPlayerToTeam(assignedTeam);
        matchRepository.save(match);

        // Notification
        notificationService.notifyPlayerJoined(match, user);

        // Vérifier si le match peut démarrer automatiquement
        if (match.getAutoStart() && match.isFull()) {
            confirmMatch(matchId);
        }

        logger.info("User {} successfully joined match {} in {}", userId, matchId, assignedTeam);
        return savedPlayer;
    }

    /**
     * Quitte un match
     */
    public void leaveMatch(UUID matchId, UUID userId) {
        logger.info("User {} leaving match {}", userId, matchId);

        Match match = getMatchById(matchId);

        // Vérifier que le match n'a pas commencé
        if (match.getStatus() != MatchStatus.FORMING) {
            throw new BusinessValidationException("Cannot leave a match that has already started");
        }

        // Trouver le joueur
        MatchPlayer playerToRemove = match.getPlayers().stream()
                .filter(mp -> mp.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this match"));

        // Vérifier que ce n'est pas le créateur (optionnel)
        if (match.getCreator().getId().equals(userId)) {
            throw new BusinessValidationException("Match creator cannot leave the match. Cancel it instead.");
        }

        // Retirer le joueur
        String team = playerToRemove.getTeam();
        matchPlayerRepository.delete(playerToRemove);

        // Mettre à jour les compteurs
        match.removePlayerFromTeam(team);
        matchRepository.save(match);

        // Notification
        notificationService.notifyPlayerLeft(match, playerToRemove.getUser());

        logger.info("User {} left match {}", userId, matchId);
    }

    /**
     * Confirme un match (passe en statut CONFIRMED)
     */
    public Match confirmMatch(UUID matchId) {
        logger.info("Confirming match {}", matchId);

        Match match = getMatchById(matchId);

        if (match.getStatus() != MatchStatus.FORMING) {
            throw new BusinessValidationException("Match is not in forming status");
        }

        if (!match.canStart()) {
            throw new BusinessValidationException("Not enough players to start the match");
        }

        match.setStatus(MatchStatus.CONFIRMED);
        Match confirmedMatch = matchRepository.save(match);

        // Notifier tous les joueurs
        notificationService.notifyMatchConfirmed(confirmedMatch);

        logger.info("Match {} confirmed", matchId);
        return confirmedMatch;
    }

    /**
     * Démarre un match
     */
    public Match startMatch(UUID matchId) {
        logger.info("Starting match {}", matchId);

        Match match = getMatchById(matchId);

        if (match.getStatus() != MatchStatus.CONFIRMED) {
            throw new BusinessValidationException("Match must be confirmed before starting");
        }

        match.startMatch();
        Match startedMatch = matchRepository.save(match);

        // Notifier tous les joueurs
        notificationService.notifyMatchStarting(startedMatch);

        logger.info("Match {} started", matchId);
        return startedMatch;
    }

    /**
     * Complète un match avec les scores
     */
    public Match completeMatch(UUID matchId, Integer scoreTeamA, Integer scoreTeamB) {
        logger.info("Completing match {} with scores A:{} B:{}", matchId, scoreTeamA, scoreTeamB);

        Match match = getMatchById(matchId);

        if (match.getStatus() != MatchStatus.IN_PROGRESS) {
            throw new BusinessValidationException("Match must be in progress to complete");
        }

        match.completeMatch(scoreTeamA, scoreTeamB);
        Match completedMatch = matchRepository.save(match);

        // Notifier tous les joueurs des résultats
        notificationService.notifyMatchCompleted(completedMatch);

        logger.info("Match {} completed. Winner: {}", matchId, match.getWinnerTeam());
        return completedMatch;
    }

    /**
     * Annule un match
     */
    public Match cancelMatch(UUID matchId, String reason) {
        logger.info("Cancelling match {} for reason: {}", matchId, reason);

        Match match = getMatchById(matchId);

        if (match.getStatus() == MatchStatus.COMPLETED) {
            throw new BusinessValidationException("Cannot cancel a completed match");
        }

        if (match.getStatus() == MatchStatus.IN_PROGRESS) {
            throw new BusinessValidationException("Cannot cancel a match in progress");
        }

        match.setStatus(MatchStatus.CANCELLED);
        Match cancelledMatch = matchRepository.save(match);

        // TODO: Gérer les remboursements si nécessaire

        // Notifier tous les joueurs
        notificationService.notifyMatchCancelled(cancelledMatch, reason);

        logger.info("Match {} cancelled", matchId);
        return cancelledMatch;
    }

    /**
     * Récupère un match par ID
     */
    @Transactional(readOnly = true)
    public Match getMatchById(UUID matchId) {
        return matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match", matchId.toString()));
    }

    /**
     * Recherche des matchs disponibles pour rejoindre
     */
    @Transactional(readOnly = true)
    public Page<Match> findAvailableMatches(MatchType matchType, SkillLevel skillLevel,
                                            LocalDateTime startDate, LocalDateTime endDate,
                                            UUID establishmentId, Pageable pageable) {

        logger.debug("Searching for available matches");

        // Pour l'instant, une implémentation simple
        // TODO: Implémenter une recherche plus sophistiquée avec critères

        List<Match> allMatches = matchRepository.findAll();

        List<Match> filteredMatches = allMatches.stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getIsPublic())
                .filter(m -> !m.isFull())
                .filter(m -> matchType == null || m.getMatchType() == matchType)
                .filter(m -> skillLevel == null || m.getSkillLevel() == skillLevel)
                .filter(m -> startDate == null || m.getSlot().getStartTime().isAfter(startDate))
                .filter(m -> endDate == null || m.getSlot().getStartTime().isBefore(endDate))
                .filter(m -> establishmentId == null ||
                        m.getField().getEstablishment().getId().equals(establishmentId))
                .collect(Collectors.toList());

        // Pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredMatches.size());

        List<Match> pageContent = filteredMatches.subList(start, end);

        return new PageImpl<>(pageContent, pageable, filteredMatches.size());
    }

    /**
     * Récupère les matchs d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Match> getUserMatches(UUID userId) {
        return matchRepository.findAll().stream()
                .filter(m -> m.getPlayers().stream()
                        .anyMatch(mp -> mp.getUser().getId().equals(userId)))
                .collect(Collectors.toList());
    }

    /**
     * Récupère les matchs à venir d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Match> getUserUpcomingMatches(UUID userId) {
        return getUserMatches(userId).stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING ||
                        m.getStatus() == MatchStatus.CONFIRMED)
                .filter(m -> m.getSlot().getStartTime().isAfter(LocalDateTime.now()))
                .collect(Collectors.toList());
    }

    /**
     * Algorithme de matchmaking intelligent
     */
    public List<Match> suggestMatches(UUID userId, MatchType preferredType,
                                      SkillLevel skillLevel, int maxResults) {

        logger.info("Suggesting matches for user {}", userId);
        return matchmakingService.getPersonalizedSuggestions(userId, maxResults);
    }

    /**
     * Trouve le meilleur match automatiquement
     */
    public Match findBestMatch(UUID userId, MatchType matchType, SkillLevel skillLevel, String location) {
        logger.info("Finding best match for user {}", userId);
        return matchmakingService.findBestMatch(userId, matchType, skillLevel, location);
    }

    /**
     * Recherche avancée de matchs avec filtres
     */
    @Transactional(readOnly = true)
    public Page<Match> searchMatches(MatchType matchType, SkillLevel skillLevel, 
                                   LocalDateTime startDate, LocalDateTime endDate,
                                   MatchStatus status, Boolean publicOnly, Pageable pageable) {
        
        logger.debug("Searching matches with advanced filters");

        List<Match> allMatches = matchRepository.findAll();

        List<Match> filteredMatches = allMatches.stream()
                .filter(m -> status == null || m.getStatus() == status)
                .filter(m -> !publicOnly || m.getIsPublic())
                .filter(m -> matchType == null || m.getMatchType() == matchType)
                .filter(m -> skillLevel == null || m.getSkillLevel() == skillLevel)
                .filter(m -> startDate == null || m.getSlot().getStartTime().isAfter(startDate))
                .filter(m -> endDate == null || m.getSlot().getStartTime().isBefore(endDate))
                .collect(Collectors.toList());

        // Pagination manuelle
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), filteredMatches.size());

        List<Match> pageContent = filteredMatches.subList(start, end);
        return new PageImpl<>(pageContent, pageable, filteredMatches.size());
    }

    /**
     * Rejoindre un match avec équipe préférée
     */
    public Match joinMatch(UUID matchId, UUID userId, String preferredTeam) {
        joinMatch(matchId, userId, preferredTeam, null);
        return getMatchById(matchId);
    }

    /**
     * Quitter un match - version publique
     */
    public Match leaveMatchAndReturn(UUID matchId, UUID userId) {
        leaveMatch(matchId, userId);
        return getMatchById(matchId);
    }

    /**
     * Démarrer un match avec vérification de permissions
     */
    public Match startMatchWithPermissions(UUID matchId, UUID userId) {
        logger.info("Starting match {} by user {}", matchId, userId);

        Match match = getMatchById(matchId);

        // Vérifier que l'utilisateur peut démarrer le match (créateur ou admin)
        if (!match.getCreator().getId().equals(userId)) {
            throw new BusinessValidationException("Only the match creator can start the match");
        }

        return startMatch(matchId);
    }

    /**
     * Terminer un match avec vérification de permissions
     */
    public Match completeMatchWithPermissions(UUID matchId, Integer scoreTeamA, Integer scoreTeamB, UUID userId) {
        logger.info("Completing match {} with scores A:{} B:{} by user {}", matchId, scoreTeamA, scoreTeamB, userId);

        Match match = getMatchById(matchId);

        // Vérifier que l'utilisateur peut terminer le match (créateur ou joueur du match)
        boolean isCreator = match.getCreator().getId().equals(userId);
        boolean isPlayer = match.getPlayers().stream()
                .anyMatch(mp -> mp.getUser().getId().equals(userId));

        if (!isCreator && !isPlayer) {
            throw new BusinessValidationException("Only match participants can update the score");
        }

        return completeMatch(matchId, scoreTeamA, scoreTeamB);
    }

    /**
     * Nettoyage automatique des matchs expirés
     */
    @Scheduled(fixedRate = 600000) // Toutes les 10 minutes
    @Transactional
    public void cleanupExpiredMatches() {
        logger.info("Starting cleanup of expired matches");

        LocalDateTime now = LocalDateTime.now();

        // Annuler les matchs non confirmés dont le créneau est passé
        List<Match> expiredMatches = matchRepository.findAll().stream()
                .filter(m -> m.getStatus() == MatchStatus.FORMING)
                .filter(m -> m.getSlot().getStartTime().isBefore(now))
                .collect(Collectors.toList());

        for (Match match : expiredMatches) {
            match.setStatus(MatchStatus.CANCELLED);
            matchRepository.save(match);
            logger.info("Cancelled expired match: {}", match.getId());
        }

        logger.info("Cleaned up {} expired matches", expiredMatches.size());
    }

    // Méthodes de validation privées

    private void validateMatchCreation(Field field, Slot slot, MatchType matchType) {
        if (!field.isAvailable()) {
            throw new BusinessValidationException("Field is not available");
        }

        if (!slot.isAvailable()) {
            throw new BusinessValidationException("Slot is not available");
        }

        if (slot.isInPast()) {
            throw new BusinessValidationException("Cannot create match for past slot");
        }

        // Vérifier la capacité du terrain
        int requiredCapacity = getRequiredCapacity(matchType);
        if (field.getCapacity() < requiredCapacity) {
            throw new BusinessValidationException("Field capacity insufficient for this match type");
        }
    }

    private void validateJoinMatch(Match match, User user) {
        if (!user.isEnabled()) {
            throw new BusinessValidationException("User account is disabled");
        }

        if (match.getStatus() != MatchStatus.FORMING) {
            throw new BusinessValidationException("Match is not open for registration");
        }

        if (!match.isRegistrationOpen()) {
            throw new BusinessValidationException("Registration deadline has passed");
        }

        if (match.isFull()) {
            throw new BusinessValidationException("Match is full");
        }
    }

    private int getRequiredCapacity(MatchType matchType) {
        switch (matchType) {
            case FIVE_VS_FIVE:
                return 10;
            case SEVEN_VS_SEVEN:
                return 14;
            case ELEVEN_VS_ELEVEN:
                return 22;
            default:
                return 10;
        }
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }
}