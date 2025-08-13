package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Payment;
import com.footArena.booking.domain.enums.PaymentMethod;
import com.footArena.booking.domain.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    // Recherche par référence de transaction
    Optional<Payment> findByTransactionReference(String transactionReference);

    // Recherche par Stripe Payment Intent ID
    Optional<Payment> findByStripePaymentIntentId(String stripePaymentIntentId);

    // Recherche par Stripe Session ID
    Optional<Payment> findByStripeSessionId(String stripeSessionId);

    // Paiements d'une réservation
    List<Payment> findByBookingIdOrderByCreatedAtDesc(UUID bookingId);

    // Paiements par statut
    List<Payment> findByStatus(PaymentStatus status);

    // Paiements par méthode
    List<Payment> findByPaymentMethod(PaymentMethod paymentMethod);

    // Paiements en attente
    @Query("SELECT p FROM Payment p WHERE p.status IN ('PENDING', 'PROCESSING')")
    List<Payment> findPendingPayments();

    // Paiements échoués récents
    @Query("SELECT p FROM Payment p WHERE p.status = 'FAILED' AND p.createdAt > :since")
    List<Payment> findRecentFailedPayments(@Param("since") LocalDateTime since);

    // Paiements à rembourser
    @Query("SELECT p FROM Payment p WHERE p.status = 'COMPLETED' AND p.refundAmount IS NULL")
    List<Payment> findRefundablePayments();

    // Revenus par période
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Revenus par méthode de paiement
    @Query("SELECT p.paymentMethod, SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate GROUP BY p.paymentMethod")
    List<Object[]> findRevenueByPaymentMethod(@Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate);

    // Paiements par établissement
    @Query("SELECT p FROM Payment p WHERE p.booking.slot.field.establishment.id = :establishmentId")
    Page<Payment> findByEstablishmentId(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    // Statistiques de remboursements
    @Query("SELECT COUNT(p), SUM(p.refundAmount) FROM Payment p WHERE p.refundAmount IS NOT NULL AND p.refundedAt BETWEEN :startDate AND :endDate")
    Object[] findRefundStatistics(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Paiements expirés
    @Query("SELECT p FROM Payment p WHERE p.status = 'PENDING' AND p.createdAt < :cutoffTime")
    List<Payment> findExpiredPayments(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Montant total des paiements d'un utilisateur
    @Query("SELECT SUM(p.amount) FROM Payment p WHERE p.booking.user.id = :userId AND p.status = 'COMPLETED'")
    BigDecimal calculateTotalPaidByUser(@Param("userId") UUID userId);

    // Paiements suspects (montants élevés)
    @Query("SELECT p FROM Payment p WHERE p.amount > :threshold AND p.status = 'COMPLETED'")
    List<Payment> findHighValuePayments(@Param("threshold") BigDecimal threshold);

    // Paiements quotidiens
    @Query("SELECT DATE(p.processedAt), COUNT(p), SUM(p.amount) FROM Payment p WHERE p.status = 'COMPLETED' AND p.processedAt BETWEEN :startDate AND :endDate GROUP BY DATE(p.processedAt) ORDER BY DATE(p.processedAt)")
    List<Object[]> findDailyPaymentStats(@Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    // Recherche avec filtres
    @Query("SELECT p FROM Payment p WHERE " +
            "(:status IS NULL OR p.status = :status) AND " +
            "(:paymentMethod IS NULL OR p.paymentMethod = :paymentMethod) AND " +
            "(:startDate IS NULL OR p.createdAt >= :startDate) AND " +
            "(:endDate IS NULL OR p.createdAt <= :endDate) AND " +
            "(:minAmount IS NULL OR p.amount >= :minAmount) AND " +
            "(:maxAmount IS NULL OR p.amount <= :maxAmount)")
    Page<Payment> findPaymentsWithFilters(@Param("status") PaymentStatus status,
                                          @Param("paymentMethod") PaymentMethod paymentMethod,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("minAmount") BigDecimal minAmount,
                                          @Param("maxAmount") BigDecimal maxAmount,
                                          Pageable pageable);
}