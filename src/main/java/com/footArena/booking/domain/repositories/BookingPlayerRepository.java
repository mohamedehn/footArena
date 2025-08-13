package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.BookingPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface BookingPlayerRepository extends JpaRepository<BookingPlayer, UUID> {

    // Joueurs d'une réservation
    List<BookingPlayer> findByBookingIdOrderByJoinedAt(UUID bookingId);

    // Joueurs confirmés d'une réservation
    List<BookingPlayer> findByBookingIdAndStatus(UUID bookingId, String status);

    // Réservations d'un utilisateur en tant que joueur
    List<BookingPlayer> findByUserIdOrderByJoinedAtDesc(UUID userId);

    // Capitaines d'une réservation
    List<BookingPlayer> findByBookingIdAndIsCaptainTrue(UUID bookingId);

    // Vérifier si un utilisateur est déjà dans une réservation
    boolean existsByBookingIdAndUserId(UUID bookingId, UUID userId);

    // Compter les joueurs confirmés d'une réservation
    @Query("SELECT COUNT(bp) FROM BookingPlayer bp WHERE bp.booking.id = :bookingId AND bp.status = 'CONFIRMED'")
    Long countConfirmedPlayersByBooking(@Param("bookingId") UUID bookingId);

    // Joueurs par équipe dans une réservation
    List<BookingPlayer> findByBookingIdAndTeamSide(UUID bookingId, String teamSide);

    // Supprimer tous les joueurs d'une réservation
    void deleteByBookingId(UUID bookingId);

    // Joueurs les plus actifs
    @Query("SELECT bp.user.id, COUNT(bp) FROM BookingPlayer bp WHERE bp.status = 'CONFIRMED' GROUP BY bp.user.id ORDER BY COUNT(bp) DESC")
    List<Object[]> findMostActiveUsers();
}