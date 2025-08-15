package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.request.CreateMatchRequest;
import com.footArena.booking.api.dto.request.JoinMatchRequest;
import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.MatchResponse;
import com.footArena.booking.api.dto.response.PageResponse;
import com.footArena.booking.api.mappers.MatchMapper;
import com.footArena.booking.domain.entities.Match;
import com.footArena.booking.domain.enums.MatchStatus;
import com.footArena.booking.domain.enums.MatchType;
import com.footArena.booking.domain.enums.SkillLevel;
import com.footArena.booking.domain.services.MatchService;
import com.footArena.booking.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/matches")
@Tag(name = "Matches", description = "Gestion du matchmaking")
public class MatchController {

    private static final Logger logger = LoggerFactory.getLogger(MatchController.class);

    private final MatchService matchService;
    private final MatchMapper matchMapper;
    private final AuthService authService;

    public MatchController(MatchService matchService, MatchMapper matchMapper, AuthService authService) {
        this.matchService = matchService;
        this.matchMapper = matchMapper;
        this.authService = authService;
    }

    @Operation(summary = "Créer un nouveau match",
            description = "Crée un nouveau match avec recherche de joueurs")
    @PostMapping
    public ResponseEntity<ApiResponse<MatchResponse>> createMatch(
            @Valid @RequestBody CreateMatchRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Creating match for user: {} slot: {}", userId, request.getSlotId());

        Match match = matchService.createMatch(
                userId,
                request.getFieldId(),
                request.getSlotId(),
                request.getTitle(),
                request.getDescription(),
                request.getMatchType(),
                request.getSkillLevel(),
                request.getIsPublic(),
                request.getEntryFee()
        );

        MatchResponse response = matchMapper.toResponse(match);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Match créé avec succès", response));
    }

    @Operation(summary = "Rechercher des matchs disponibles")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<PageResponse<MatchResponse>>> searchMatches(
            @Parameter(description = "Type de match") @RequestParam(required = false) MatchType matchType,
            @Parameter(description = "Niveau de compétence") @RequestParam(required = false) SkillLevel skillLevel,
            @Parameter(description = "Date de début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "Statut du match") @RequestParam(required = false) MatchStatus status,
            @Parameter(description = "Matchs publics uniquement") @RequestParam(defaultValue = "true") Boolean publicOnly,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size) {

        logger.debug("Searching matches with filters");

        Sort sort = Sort.by("slot.startTime").ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Match> matches = matchService.searchMatches(matchType, skillLevel, startDate, endDate, status, publicOnly, pageable);
        Page<MatchResponse> responsePage = matches.map(matchMapper::toResponse);
        PageResponse<MatchResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Recherche effectuée", pageResponse));
    }

    @Operation(summary = "Rejoindre un match")
    @PostMapping("/{id}/join")
    public ResponseEntity<ApiResponse<MatchResponse>> joinMatch(
            @Parameter(description = "ID du match") @PathVariable UUID id,
            @Valid @RequestBody JoinMatchRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("User {} joining match: {} in team: {}", userId, id, request.getPreferredTeam());

        Match match = matchService.joinMatch(id, userId, request.getPreferredTeam());
        MatchResponse response = matchMapper.toResponse(match);

        return ResponseEntity.ok(ApiResponse.success("Vous avez rejoint le match", response));
    }

    @Operation(summary = "Quitter un match")
    @PostMapping("/{id}/leave")
    public ResponseEntity<ApiResponse<MatchResponse>> leaveMatch(
            @Parameter(description = "ID du match") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("User {} leaving match: {}", userId, id);

        Match match = matchService.leaveMatchAndReturn(id, userId);
        MatchResponse response = matchMapper.toResponse(match);

        return ResponseEntity.ok(ApiResponse.success("Vous avez quitté le match", response));
    }

    @Operation(summary = "Récupérer les détails d'un match")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MatchResponse>> getMatchById(
            @Parameter(description = "ID du match") @PathVariable UUID id) {

        logger.debug("Fetching match: {}", id);

        Match match = matchService.getMatchById(id);
        MatchResponse response = matchMapper.toResponse(match);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer mes matchs")
    @GetMapping("/my-matches")
    public ResponseEntity<ApiResponse<List<MatchResponse>>> getMyMatches(
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching matches for user: {}", userId);

        List<Match> matches = matchService.getUserMatches(userId);
        List<MatchResponse> responses = matchMapper.toResponseList(matches);

        return ResponseEntity.ok(ApiResponse.success("Matchs récupérés", responses));
    }

    @Operation(summary = "Démarrer un match")
    @PostMapping("/{id}/start")
    public ResponseEntity<ApiResponse<MatchResponse>> startMatch(
            @Parameter(description = "ID du match") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Starting match: {} by user: {}", id, userId);

        Match match = matchService.startMatchWithPermissions(id, userId);
        MatchResponse response = matchMapper.toResponse(match);

        return ResponseEntity.ok(ApiResponse.success("Match démarré", response));
    }

    @Operation(summary = "Terminer un match")
    @PostMapping("/{id}/complete")
    public ResponseEntity<ApiResponse<MatchResponse>> completeMatch(
            @Parameter(description = "ID du match") @PathVariable UUID id,
            @Parameter(description = "Score équipe A") @RequestParam Integer scoreTeamA,
            @Parameter(description = "Score équipe B") @RequestParam Integer scoreTeamB,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Completing match: {} with scores A:{} B:{}", id, scoreTeamA, scoreTeamB);

        Match match = matchService.completeMatchWithPermissions(id, scoreTeamA, scoreTeamB, userId);
        MatchResponse response = matchMapper.toResponse(match);

        return ResponseEntity.ok(ApiResponse.success("Match terminé", response));
    }

    @Operation(summary = "Matchmaking automatique")
    @PostMapping("/auto-match")
    public ResponseEntity<ApiResponse<MatchResponse>> findAutoMatch(
            @Parameter(description = "Type de match") @RequestParam MatchType matchType,
            @Parameter(description = "Niveau de compétence") @RequestParam SkillLevel skillLevel,
            @Parameter(description = "Localisation") @RequestParam(required = false) String location,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Auto-matching for user: {} type: {} level: {}", userId, matchType, skillLevel);

        Match match = matchService.findBestMatch(userId, matchType, skillLevel, location);

        if (match != null) {
            MatchResponse response = matchMapper.toResponse(match);
            return ResponseEntity.ok(ApiResponse.success("Match trouvé!", response));
        } else {
            return ResponseEntity.ok(ApiResponse.error("Aucun match disponible pour le moment"));
        }
    }

    // Méthodes utilitaires privées

    private UUID getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return authService.getUserFromToken(token).getId();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("No valid token found in request");
    }
}