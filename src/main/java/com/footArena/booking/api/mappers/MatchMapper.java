package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.MatchPlayerResponse;
import com.footArena.booking.api.dto.response.MatchResponse;
import com.footArena.booking.domain.entities.Match;
import com.footArena.booking.domain.entities.MatchPlayer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MatchMapper {

    private final UserMapper userMapper;
    private final FieldMapper fieldMapper;
    private final SlotMapper slotMapper;

    public MatchMapper(UserMapper userMapper, FieldMapper fieldMapper, SlotMapper slotMapper) {
        this.userMapper = userMapper;
        this.fieldMapper = fieldMapper;
        this.slotMapper = slotMapper;
    }

    /**
     * Convertit une entité Match en MatchResponse
     */
    public MatchResponse toResponse(Match match) {
        if (match == null) {
            return null;
        }

        MatchResponse response = new MatchResponse();

        // Informations de base
        response.setId(match.getId());
        response.setTitle(match.getTitle());
        response.setDescription(match.getDescription());
        response.setMatchType(match.getMatchType());
        response.setSkillLevel(match.getSkillLevel());
        response.setStatus(match.getStatus());
        response.setIsPublic(match.getIsPublic());

        // Informations sur les équipes
        response.setMaxPlayersPerTeam(match.getMaxPlayersPerTeam());
        response.setCurrentPlayersTeamA(match.getCurrentPlayersTeamA());
        response.setCurrentPlayersTeamB(match.getCurrentPlayersTeamB());
        response.setMinPlayersToStart(match.getMinPlayersToStart());

        // Configuration
        response.setAutoStart(match.getAutoStart());
        response.setAllowSubstitutes(match.getAllowSubstitutes());
        response.setEntryFee(match.getEntryFee());
        response.setRegistrationDeadline(match.getRegistrationDeadline());

        // Données temporelles
        response.setStartedAt(match.getStartedAt());
        response.setCompletedAt(match.getCompletedAt());
        response.setCreatedAt(match.getCreatedAt());
        response.setUpdatedAt(match.getUpdatedAt());

        // Résultats
        response.setScoreTeamA(match.getScoreTeamA());
        response.setScoreTeamB(match.getScoreTeamB());
        response.setWinnerTeam(match.getWinnerTeam());

        // Relations
        if (match.getField() != null) {
            response.setField(fieldMapper.toResponse(match.getField()));
            response.setLocation(match.getField().getLocation());
        }

        if (match.getSlot() != null) {
            response.setSlot(slotMapper.toSimpleResponse(match.getSlot()));
            // Calculer la durée du match
            Duration duration = Duration.between(match.getSlot().getStartTime(), match.getSlot().getEndTime());
            response.setMatchDuration(formatDuration(duration));
        }

        if (match.getCreator() != null) {
            response.setCreator(userMapper.toPublicResponse(match.getCreator()));
        }

        // Joueurs
        if (match.getPlayers() != null && !match.getPlayers().isEmpty()) {
            response.setPlayers(match.getPlayers().stream()
                    .map(this::toMatchPlayerResponse)
                    .collect(Collectors.toList()));
        }

        // Champs calculés
        response.setIsFull(match.isFull());
        response.setCanStart(match.canStart());
        response.setTotalPlayers(match.getTotalPlayers());
        response.setAvailableSpots(match.getAvailableSpots());
        response.setIsRegistrationOpen(match.isRegistrationOpen());

        return response;
    }

    /**
     * Convertit une entité Match en MatchResponse simplifié (sans relations lourdes)
     */
    public MatchResponse toSimpleResponse(Match match) {
        if (match == null) {
            return null;
        }

        MatchResponse response = new MatchResponse();

        // Informations essentielles seulement
        response.setId(match.getId());
        response.setTitle(match.getTitle());
        response.setMatchType(match.getMatchType());
        response.setSkillLevel(match.getSkillLevel());
        response.setStatus(match.getStatus());
        response.setIsPublic(match.getIsPublic());

        // Informations sur les équipes
        response.setMaxPlayersPerTeam(match.getMaxPlayersPerTeam());
        response.setCurrentPlayersTeamA(match.getCurrentPlayersTeamA());
        response.setCurrentPlayersTeamB(match.getCurrentPlayersTeamB());

        // Champs calculés importants
        response.setIsFull(match.isFull());
        response.setTotalPlayers(match.getTotalPlayers());
        response.setAvailableSpots(match.getAvailableSpots());
        response.setIsRegistrationOpen(match.isRegistrationOpen());

        // Slot simplifié
        if (match.getSlot() != null) {
            response.setSlot(slotMapper.toSimpleResponse(match.getSlot()));
        }

        // Field basique (nom seulement)
        if (match.getField() != null) {
            response.setLocation(match.getField().getName() + " - " + match.getField().getLocation());
        }

        response.setCreatedAt(match.getCreatedAt());

        return response;
    }

    /**
     * Convertit une liste d'entités Match en liste de MatchResponse
     */
    public List<MatchResponse> toResponseList(List<Match> matches) {
        if (matches == null) {
            return List.of();
        }

        return matches.stream()
                .map(this::toSimpleResponse) // Utilisation de la version simplifiée pour les listes
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité MatchPlayer en MatchPlayerResponse
     */
    public MatchPlayerResponse toMatchPlayerResponse(MatchPlayer matchPlayer) {
        if (matchPlayer == null) {
            return null;
        }

        MatchPlayerResponse response = new MatchPlayerResponse();
        response.setId(matchPlayer.getId());
        response.setTeam(matchPlayer.getTeam());

        // Mapper l'utilisateur
        if (matchPlayer.getUser() != null) {
            response.setUser(userMapper.toPublicResponse(matchPlayer.getUser()));
            // Utiliser le nom complet de l'utilisateur comme nom de joueur par défaut
            response.setPlayerName(matchPlayer.getUser().getFullName());
        }

        // Position (si définie dans l'entité)
        // Comme l'entité MatchPlayer actuelle n'a pas de champ position,
        // on pourrait l'ajouter ou laisser null pour l'instant
        response.setPosition(null); // À implémenter si nécessaire

        // Déterminer si c'est le capitaine (créateur du match)
        if (matchPlayer.getMatch() != null &&
                matchPlayer.getMatch().getCreator() != null &&
                matchPlayer.getUser() != null &&
                matchPlayer.getMatch().getCreator().getId().equals(matchPlayer.getUser().getId())) {
            response.setIsCaptain(true);
        } else {
            response.setIsCaptain(false);
        }

        // Status par défaut
        response.setStatus("CONFIRMED");

        // Date d'inscription (non disponible dans l'entité actuelle, utiliser createdAt du match)
        if (matchPlayer.getMatch() != null) {
            response.setJoinedAt(matchPlayer.getMatch().getCreatedAt());
        }

        return response;
    }

    /**
     * Convertit une liste de MatchPlayer en liste de MatchPlayerResponse
     */
    public List<MatchPlayerResponse> toMatchPlayerResponseList(List<MatchPlayer> players) {
        if (players == null) {
            return List.of();
        }

        return players.stream()
                .map(this::toMatchPlayerResponse)
                .collect(Collectors.toList());
    }

    /**
     * Formate une durée en chaîne lisible
     */
    private String formatDuration(Duration duration) {
        long hours = duration.toHours();
        long minutes = duration.toMinutesPart();

        if (hours > 0) {
            return String.format("%dh %dmin", hours, minutes);
        } else {
            return String.format("%dmin", minutes);
        }
    }
}