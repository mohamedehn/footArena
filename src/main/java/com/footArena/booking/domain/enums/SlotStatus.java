package com.footArena.booking.domain.enums;

/**
 * Statuts possibles pour un créneau horaire
 */
public enum SlotStatus {
    /**
     * Créneau disponible pour réservation
     */
    AVAILABLE,

    /**
     * Créneau réservé (au moins une réservation)
     */
    RESERVED,

    /**
     * Créneau complet (capacité maximale atteinte)
     */
    FULL,

    /**
     * Créneau en maintenance (terrain indisponible)
     */
    MAINTENANCE,

    /**
     * Créneau annulé (météo, événement spécial, etc.)
     */
    CANCELLED
}