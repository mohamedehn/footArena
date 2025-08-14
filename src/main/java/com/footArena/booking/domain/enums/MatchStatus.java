package com.footArena.booking.domain.enums;

public enum MatchStatus {
    /**
     * Match en cours de formation (recherche de joueurs)
     */
    FORMING,

    /**
     * Match confirmé, en attente du début
     */
    CONFIRMED,

    /**
     * Match en cours
     */
    IN_PROGRESS,

    /**
     * Match terminé
     */
    COMPLETED,

    /**
     * Match annulé
     */
    CANCELLED,

    /**
     * Match reporté
     */
    POSTPONED
}