package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.BookingPlayerResponse;
import com.footArena.booking.api.dto.response.BookingResponse;
import com.footArena.booking.api.dto.response.PaymentResponse;
import com.footArena.booking.domain.entities.Booking;
import com.footArena.booking.domain.entities.BookingPlayer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class BookingMapper {

    private final UserMapper userMapper;
    private final SlotMapper slotMapper;
    private final PaymentMapper paymentMapper;

    public BookingMapper(UserMapper userMapper, SlotMapper slotMapper, PaymentMapper paymentMapper) {
        this.userMapper = userMapper;
        this.slotMapper = slotMapper;
        this.paymentMapper = paymentMapper;
    }

    /**
     * Convertit une entité Booking en BookingResponse
     */
    public BookingResponse toResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setUser(userMapper.toResponse(booking.getUser()));
        response.setSlot(slotMapper.toResponse(booking.getSlot()));
        response.setBookingType(booking.getBookingType());
        response.setStatus(booking.getStatus());
        response.setNumberOfPlayers(booking.getNumberOfPlayers());
        response.setTotalAmount(booking.getTotalAmount());
        response.setPaidAmount(booking.getTotalPaidAmount());
        response.setRemainingAmount(booking.getRemainingAmount());
        response.setTeamName(booking.getTeamName());
        response.setSpecialRequests(booking.getSpecialRequests());
        response.setContactPhone(booking.getContactPhone());
        response.setConfirmationDeadline(booking.getConfirmationDeadline());
        response.setConfirmedAt(booking.getConfirmedAt());
        response.setCancelledAt(booking.getCancelledAt());
        response.setCancellationReason(booking.getCancellationReason());
        response.setIsPaid(booking.getIsPaid());
        response.setCanBeCancelled(booking.canBeCancelled());
        response.setIsExpired(booking.isExpired());
        response.setCreatedAt(booking.getCreatedAt());
        response.setUpdatedAt(booking.getUpdatedAt());

        // Convertir les joueurs
        if (booking.getPlayers() != null) {
            response.setPlayers(booking.getPlayers().stream()
                    .map(this::toBookingPlayerResponse)
                    .collect(Collectors.toList()));
        }

        // Convertir les paiements
        if (booking.getPayments() != null) {
            response.setPayments(booking.getPayments().stream()
                    .map(paymentMapper::toResponse)
                    .collect(Collectors.toList()));
        }

        return response;
    }

    /**
     * Convertit une entité Booking en BookingResponse simplifié (sans relations lourdes)
     */
    public BookingResponse toSimpleResponse(Booking booking) {
        if (booking == null) {
            return null;
        }

        BookingResponse response = new BookingResponse();
        response.setId(booking.getId());
        response.setBookingReference(booking.getBookingReference());
        response.setUser(userMapper.toPublicResponse(booking.getUser()));
        response.setSlot(slotMapper.toSimpleResponse(booking.getSlot()));
        response.setBookingType(booking.getBookingType());
        response.setStatus(booking.getStatus());
        response.setNumberOfPlayers(booking.getNumberOfPlayers());
        response.setTotalAmount(booking.getTotalAmount());
        response.setTeamName(booking.getTeamName());
        response.setConfirmationDeadline(booking.getConfirmationDeadline());
        response.setIsPaid(booking.getIsPaid());
        response.setCanBeCancelled(booking.canBeCancelled());
        response.setIsExpired(booking.isExpired());
        response.setCreatedAt(booking.getCreatedAt());

        return response;
    }

    /**
     * Convertit une liste d'entités Booking en liste de BookingResponse
     */
    public List<BookingResponse> toResponseList(List<Booking> bookings) {
        if (bookings == null) {
            return List.of();
        }

        return bookings.stream()
                .map(this::toSimpleResponse) // Utilisation de la version simplifiée pour les listes
                .collect(Collectors.toList());
    }

    /**
     * Convertit une entité BookingPlayer en BookingPlayerResponse
     */
    public BookingPlayerResponse toBookingPlayerResponse(BookingPlayer bookingPlayer) {
        if (bookingPlayer == null) {
            return null;
        }

        BookingPlayerResponse response = new BookingPlayerResponse();
        response.setId(bookingPlayer.getId());
        response.setUser(userMapper.toPublicResponse(bookingPlayer.getUser()));
        response.setPlayerName(bookingPlayer.getPlayerName());
        response.setPosition(bookingPlayer.getPosition());
        response.setTeamSide(bookingPlayer.getTeamSide());
        response.setIsCaptain(bookingPlayer.getIsCaptain());
        response.setStatus(bookingPlayer.getStatus());
        response.setJoinedAt(bookingPlayer.getJoinedAt());

        return response;
    }

    /**
     * Convertit une liste de BookingPlayer en liste de BookingPlayerResponse
     */
    public List<BookingPlayerResponse> toBookingPlayerResponseList(List<BookingPlayer> players) {
        if (players == null) {
            return List.of();
        }

        return players.stream()
                .map(this::toBookingPlayerResponse)
                .collect(Collectors.toList());
    }
}