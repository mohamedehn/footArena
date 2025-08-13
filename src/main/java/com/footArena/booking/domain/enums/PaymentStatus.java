package com.footArena.booking.domain.enums;

/**
 * Statuts des paiements
 */
public enum PaymentStatus {
    /**
     * Paiement en attente de traitement
     */
    PENDING,

    /**
     * Paiement en cours de traitement
     */
    PROCESSING,

    /**
     * Paiement réussi et validé
     */
    COMPLETED,

    /**
     * Paiement échoué (carte refusée, fonds insuffisants, etc.)
     */
    FAILED,

    /**
     * Paiement annulé par l'utilisateur
     */
    CANCELLED,

    /**
     * Paiement remboursé
     */
    REFUNDED,

    /**
     * Remboursement partiel
     */
    PARTIALLY_REFUNDED,

    /**
     * Paiement expiré (timeout)
     */
    EXPIRED
}