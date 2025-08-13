package com.footArena.booking.domain.enums;

/**
 * Méthodes de paiement disponibles
 */
public enum PaymentMethod {
    /**
     * Paiement par carte via Stripe
     */
    STRIPE,

    /**
     * Paiement en espèces sur place
     */
    CASH,

    /**
     * Paiement par chèque
     */
    CHECK,

    /**
     * Paiement par virement bancaire
     */
    BANK_TRANSFER,

    /**
     * Crédit utilisateur (remboursement, avoir)
     */
    CREDIT,

    /**
     * Gratuit (événement promotionnel)
     */
    FREE
}