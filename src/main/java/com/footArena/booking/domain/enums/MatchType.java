package com.footArena.booking.domain.enums;

/**
 * Types de matchs pour le matchmaking
 */
public enum MatchType {
    /**
     * Match 5 contre 5
     */
    FIVE_VS_FIVE,

    /**
     * Match 7 contre 7
     */
    SEVEN_VS_SEVEN,

    /**
     * Match 11 contre 11
     */
    ELEVEN_VS_ELEVEN,

    /**
     * Match libre (nombre flexible)
     */
    FLEXIBLE,

    /**
     * Entraînement en groupe
     */
    TRAINING,

    /**
     * Tournoi
     */
    TOURNAMENT
}