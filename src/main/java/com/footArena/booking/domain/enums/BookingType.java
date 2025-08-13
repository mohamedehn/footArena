package com.footArena.booking.domain.enums;

/**
 * Types de réservation possibles
 */
public enum BookingType {
    /**
     * Réservation individuelle (un seul joueur)
     */
    INDIVIDUAL,

    /**
     * Réservation en équipe (groupe constitué)
     */
    TEAM,

    /**
     * Réservation avec matchmaking (recherche d'adversaires)
     */
    MATCHMAKING,

    /**
     * Événement privé (anniversaire, corporate, etc.)
     */
    PRIVATE_EVENT,

    /**
     * Entraînement en groupe
     */
    TRAINING,

    /**
     * Match amical organisé
     */
    FRIENDLY_MATCH
}