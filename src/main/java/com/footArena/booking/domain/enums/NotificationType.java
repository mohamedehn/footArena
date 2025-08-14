package com.footArena.booking.domain.enums;

/**
 * Types de notifications pour le matchmaking
 */
public enum NotificationType {
    MATCH_FOUND,           // Match trouvé
    MATCH_INVITATION,      // Invitation à un match
    MATCH_CONFIRMED,       // Match confirmé
    MATCH_CANCELLED,       // Match annulé
    TEAM_INVITATION,       // Invitation d'équipe
    PLAYER_JOINED,         // Joueur a rejoint
    PLAYER_LEFT,           // Joueur a quitté
    MATCH_STARTING_SOON,   // Match commence bientôt
    MATCH_COMPLETED        // Match terminé
}