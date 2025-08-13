package com.footArena.booking.domain.enums;

/**
 * Statuts possibles pour une réservation
 */
public enum BookingStatus {
    /**
     * Réservation en attente de confirmation/paiement
     */
    PENDING,

    /**
     * Réservation confirmée et payée
     */
    CONFIRMED,

    /**
     * Réservation en attente de paiement (après confirmation)
     */
    AWAITING_PAYMENT,

    /**
     * Réservation annulée par l'utilisateur
     */
    CANCELLED,

    /**
     * Réservation annulée par l'établissement
     */
    CANCELLED_BY_ESTABLISHMENT,

    /**
     * Match terminé avec succès
     */
    COMPLETED,

    /**
     * Réservation expirée (non confirmée dans les temps)
     */
    EXPIRED,

    /**
     * Réservation refusée
     */
    REJECTED
}