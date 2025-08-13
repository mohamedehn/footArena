package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Booking;
import com.footArena.booking.domain.enums.BookingStatus;
import com.footArena.booking.domain.enums.BookingType;
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

public interface BookingRepository extends JpaRepository<Booking, UUID> {

    // Recherche par référence de réservation
    Optional<Booking> findByBookingReference(String bookingReference);

    // Réservations d'un utilisateur
    List<Booking> findByUserIdOrderByCreatedAtDesc(UUID userId);

    // Réservations par statut
    List<Booking> findByStatus(BookingStatus status);

    // Réservations d'un créneau
    List<Booking> findBySlotId(UUID slotId);

    // Réservations en attente de confirmation
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.confirmationDeadline < :deadline")
    List<Booking> findExpiredPendingBookings(@Param("deadline") LocalDateTime deadline);

    // Réservations confirmées pour aujourd'hui
    @Query("SELECT b FROM Booking b WHERE b.status = 'CONFIRMED' AND DATE(b.slot.startTime) = CURRENT_DATE")
    List<Booking> findTodayConfirmedBookings();

    // Réservations d'un utilisateur avec statut
    List<Booking> findByUserIdAndStatus(UUID userId, BookingStatus status);

    // Réservations par type
    List<Booking> findByBookingType(BookingType bookingType);

    // Réservations dans une plage de dates
    @Query("SELECT b FROM Booking b WHERE b.slot.startTime BETWEEN :startDate AND :endDate")
    List<Booking> findByDateRange(@Param("startDate") LocalDateTime startDate,
                                  @Param("endDate") LocalDateTime endDate);

    // Réservations par établissement
    @Query("SELECT b FROM Booking b WHERE b.slot.field.establishment.id = :establishmentId")
    Page<Booking> findByEstablishmentId(@Param("establishmentId") UUID establishmentId, Pageable pageable);

    // Réservations à venir d'un utilisateur
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.startTime > :now AND b.status IN :statuses ORDER BY b.slot.startTime")
    List<Booking> findUpcomingBookingsByUser(@Param("userId") UUID userId,
                                             @Param("now") LocalDateTime now,
                                             @Param("statuses") List<BookingStatus> statuses);

    // Historique des réservations d'un utilisateur
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.endTime < :now ORDER BY b.slot.startTime DESC")
    Page<Booking> findUserBookingHistory(@Param("userId") UUID userId,
                                         @Param("now") LocalDateTime now,
                                         Pageable pageable);

    // Réservations nécessitant un paiement
    @Query("SELECT b FROM Booking b WHERE b.isPaid = false AND b.status IN ('CONFIRMED', 'AWAITING_PAYMENT')")
    List<Booking> findBookingsNeedingPayment();

    // Statistiques de réservations par utilisateur
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.status = 'COMPLETED'")
    Long countCompletedBookingsByUser(@Param("userId") UUID userId);

    // Revenus par période
    @Query("SELECT SUM(b.totalAmount) FROM Booking b WHERE b.status = 'COMPLETED' AND b.slot.startTime BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Réservations populaires par terrain
    @Query("SELECT b.slot.field.id, COUNT(b) FROM Booking b WHERE b.status = 'COMPLETED' GROUP BY b.slot.field.id ORDER BY COUNT(b) DESC")
    List<Object[]> findMostPopularFields();

    // Taux d'occupation moyen
    @Query("SELECT AVG(CAST(b.numberOfPlayers AS double)) FROM Booking b WHERE b.status = 'COMPLETED' AND b.slot.field.establishment.id = :establishmentId")
    Double calculateAverageOccupancyByEstablishment(@Param("establishmentId") UUID establishmentId);

    // Réservations en conflit potentiel
    @Query("SELECT b FROM Booking b WHERE b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED') AND b.id != :excludeBookingId")
    List<Booking> findConflictingBookings(@Param("slotId") UUID slotId, @Param("excludeBookingId") UUID excludeBookingId);

    // Recherche avec filtres avancés
    @Query("SELECT b FROM Booking b WHERE " +
            "(:userId IS NULL OR b.user.id = :userId) AND " +
            "(:status IS NULL OR b.status = :status) AND " +
            "(:bookingType IS NULL OR b.bookingType = :bookingType) AND " +
            "(:startDate IS NULL OR b.slot.startTime >= :startDate) AND " +
            "(:endDate IS NULL OR b.slot.endTime <= :endDate) AND " +
            "(:establishmentId IS NULL OR b.slot.field.establishment.id = :establishmentId) AND " +
            "(:isPaid IS NULL OR b.isPaid = :isPaid)")
    Page<Booking> findBookingsWithFilters(@Param("userId") UUID userId,
                                          @Param("status") BookingStatus status,
                                          @Param("bookingType") BookingType bookingType,
                                          @Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate,
                                          @Param("establishmentId") UUID establishmentId,
                                          @Param("isPaid") Boolean isPaid,
                                          Pageable pageable);

    // Réservations à rappeler (confirmation requise)
    @Query("SELECT b FROM Booking b WHERE b.status = 'PENDING' AND b.confirmationDeadline BETWEEN :now AND :reminderTime")
    List<Booking> findBookingsNeedingReminder(@Param("now") LocalDateTime now,
                                              @Param("reminderTime") LocalDateTime reminderTime);

    // Réservations récentes pour détection de fraude
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.user.id = :userId AND b.createdAt > :sinceTime")
    Long countRecentBookingsByUser(@Param("userId") UUID userId, @Param("sinceTime") LocalDateTime sinceTime);

    // Vérification de double réservation
    @Query("SELECT b FROM Booking b WHERE b.user.id = :userId AND b.slot.id = :slotId AND b.status IN ('PENDING', 'CONFIRMED')")
    List<Booking> findExistingBookingForUserAndSlot(@Param("userId") UUID userId, @Param("slotId") UUID slotId);
}