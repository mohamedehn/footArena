package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Slot;
import com.footArena.booking.domain.enums.SlotStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface SlotRepository extends JpaRepository<Slot, UUID> {

    // Recherche par statut
    List<Slot> findByStatus(SlotStatus status);

    // Recherche par terrain
    List<Slot> findByFieldId(UUID fieldId);

    // Recherche par établissement via le terrain
    @Query("SELECT s FROM Slot s WHERE s.field.establishment.id = :establishmentId")
    List<Slot> findByEstablishmentId(@Param("establishmentId") UUID establishmentId);

    // Créneaux disponibles dans une plage de dates
    @Query("SELECT s FROM Slot s WHERE s.status = :status AND s.startTime >= :startTime AND s.endTime <= :endTime")
    List<Slot> findAvailableSlotsBetween(@Param("status") SlotStatus status,
                                         @Param("startTime") LocalDateTime startTime,
                                         @Param("endTime") LocalDateTime endTime);

    // Créneaux d'un terrain dans une plage de dates
    @Query("SELECT s FROM Slot s WHERE s.field.id = :fieldId AND s.startTime >= :startTime AND s.endTime <= :endTime ORDER BY s.startTime")
    List<Slot> findByFieldAndDateRange(@Param("fieldId") UUID fieldId,
                                       @Param("startTime") LocalDateTime startTime,
                                       @Param("endTime") LocalDateTime endTime);

    // Vérification de conflit temporel pour un terrain
    @Query("SELECT s FROM Slot s WHERE s.field.id = :fieldId AND " +
            "((s.startTime <= :startTime AND s.endTime > :startTime) OR " +
            "(s.startTime < :endTime AND s.endTime >= :endTime) OR " +
            "(s.startTime >= :startTime AND s.endTime <= :endTime))")
    List<Slot> findConflictingSlots(@Param("fieldId") UUID fieldId,
                                    @Param("startTime") LocalDateTime startTime,
                                    @Param("endTime") LocalDateTime endTime);

    // Créneaux disponibles aujourd'hui
    @Query("SELECT s FROM Slot s WHERE s.status IN :statuses AND DATE(s.startTime) = CURRENT_DATE ORDER BY s.startTime")
    List<Slot> findTodayAvailableSlots(@Param("statuses") List<SlotStatus> statuses);

    // Créneaux dans les prochaines heures
    @Query("SELECT s FROM Slot s WHERE s.status = 'AVAILABLE' AND s.startTime BETWEEN :now AND :futureTime ORDER BY s.startTime")
    List<Slot> findUpcomingAvailableSlots(@Param("now") LocalDateTime now,
                                          @Param("futureTime") LocalDateTime futureTime);

    // Recherche par prix
    List<Slot> findByPriceBetween(BigDecimal minPrice, BigDecimal maxPrice);

    // Créneaux premium
    List<Slot> findByIsPremiumTrue();

    // Créneaux avec places disponibles
    @Query("SELECT s FROM Slot s WHERE s.currentBookings < s.maxCapacity AND s.status = 'AVAILABLE'")
    List<Slot> findSlotsWithAvailableSpots();

    // Statistiques d'occupation
    @Query("SELECT AVG(CAST(s.currentBookings AS double) / s.maxCapacity * 100) FROM Slot s WHERE s.field.establishment.id = :establishmentId")
    Double calculateOccupancyRateByEstablishment(@Param("establishmentId") UUID establishmentId);

    // Créneaux les plus populaires
    @Query("SELECT s FROM Slot s WHERE s.currentBookings > 0 ORDER BY s.currentBookings DESC")
    Page<Slot> findMostPopularSlots(Pageable pageable);

    // Revenus par période
    @Query("SELECT SUM(s.price * s.currentBookings) FROM Slot s WHERE s.startTime BETWEEN :startDate AND :endDate")
    BigDecimal calculateRevenueByPeriod(@Param("startDate") LocalDateTime startDate,
                                        @Param("endDate") LocalDateTime endDate);

    // Créneaux expirés à nettoyer
    @Query("SELECT s FROM Slot s WHERE s.endTime < :cutoffTime AND s.status != 'COMPLETED'")
    List<Slot> findExpiredSlots(@Param("cutoffTime") LocalDateTime cutoffTime);

    // Recherche de créneaux avec filtres avancés
    @Query("SELECT s FROM Slot s WHERE " +
            "(:fieldId IS NULL OR s.field.id = :fieldId) AND " +
            "(:status IS NULL OR s.status = :status) AND " +
            "(:startDate IS NULL OR s.startTime >= :startDate) AND " +
            "(:endDate IS NULL OR s.endTime <= :endDate) AND " +
            "(:minPrice IS NULL OR s.price >= :minPrice) AND " +
            "(:maxPrice IS NULL OR s.price <= :maxPrice) AND " +
            "(:isPremium IS NULL OR s.isPremium = :isPremium)")
    Page<Slot> findSlotsWithFilters(@Param("fieldId") UUID fieldId,
                                    @Param("status") SlotStatus status,
                                    @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate,
                                    @Param("minPrice") BigDecimal minPrice,
                                    @Param("maxPrice") BigDecimal maxPrice,
                                    @Param("isPremium") Boolean isPremium,
                                    Pageable pageable);
}