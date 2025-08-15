package com.footArena.booking.domain.enums;

/**
 * Types de notifications pour le matchmaking
 */
public enum NotificationType {
    // Matchs
    MATCH_FOUND,                    // Match trouvé
    MATCH_INVITATION,               // Invitation à un match
    MATCH_CONFIRMED,                // Match confirmé
    MATCH_CANCELLED,                // Match annulé
    MATCH_STARTING_SOON,            // Match commence bientôt
    MATCH_COMPLETED,                // Match terminé
    MATCH_REMINDER,                 // Rappel de match
    MATCH_SUGGESTION,               // Suggestion de match intelligent
    MATCH_CONFIRMATION_REQUEST,     // Demande de confirmation
    
    // Joueurs
    PLAYER_JOINED,                  // Joueur a rejoint
    PLAYER_LEFT,                    // Joueur a quitté
    PLAYER_CONFIRMED,               // Joueur confirmé
    PLAYER_DECLINED,                // Joueur a décliné
    
    // Équipes
    TEAM_INVITATION,                // Invitation d'équipe
    TEAM_MEMBER_JOINED,             // Nouveau membre d'équipe
    TEAM_MEMBER_LEFT,               // Membre a quitté l'équipe
    TEAM_MATCH_SCHEDULED,           // Match d'équipe programmé
    
    // Système
    SYSTEM_MAINTENANCE,             // Maintenance système
    ACCOUNT_UPDATE,                 // Mise à jour de compte
    SKILL_LEVEL_UPDATE,             // Mise à jour niveau de compétence
    DAILY_DIGEST                    // Résumé quotidien
}