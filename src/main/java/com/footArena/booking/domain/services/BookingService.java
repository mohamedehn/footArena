package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Booking;
import com.footArena.booking.domain.entities.BookingPlayer;
import com.footArena.booking.domain.entities.Slot;
import com.footArena.booking.domain.entities.User;
import com.footArena.booking.domain.enums.BookingStatus;
import com.footArena.booking.domain.enums.BookingType;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.BookingPlayerRepository;
import com.footArena.booking.domain.repositories.BookingRepository;
import com.footArena.booking.domain.repositories.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class BookingService {

    private static final Logger logger = LoggerFactory.getLogger(BookingService.class);
    private static final int MAX_BOOKINGS_PER_USER_PER_DAY = 3;
    private static final int MAX_PLAYERS_PER_BOOKING = 22;

    private final BookingRepository bookingRepository;
    private final BookingPlayerRepository bookingPlayerRepository;
    private final UserRepository userRepository;
    private final SlotService slotService;

    public BookingService(BookingRepository bookingRepository,
                          BookingPlayerRepository bookingPlayerRepository,
                          UserRepository userRepository,
                          SlotService slotService) {
        this.bookingRepository = bookingRepository;
        this.bookingPlayerRepository = bookingPlayerRepository;
        this.userRepository = userRepository;
        this.slotService = slotService;
    }

    /**
     * Crée une nouvelle réservation
     */
    public Booking createBooking(UUID userId, UUID slotId, BookingType bookingType,
                                 Integer numberOfPlayers, String teamName, String specialRequests,
                                 String contactPhone) {
        logger.info("Creating booking for user: {} and slot: {}", userId, slotId);

        User user = getUserById(userId);
        Slot slot = slotService.getSlotById(slotId);

        // Validations métier
        validateBookingRequest(user, slot, bookingType, numberOfPlayers);
        checkSlotAvailability(slot, numberOfPlayers);
        checkUserBookingLimits(userId);
        checkDuplicateBooking(userId, slotId);

        // Calculer le montant total
        BigDecimal totalAmount = calculateBookingAmount(slot, numberOfPlayers, bookingType);

        // Créer la réservation
        Booking booking = new Booking(user, slot, bookingType, numberOfPlayers, totalAmount);
        booking.setTeamName(teamName);
        booking.setSpecialRequests(specialRequests);
        booking.setContactPhone(contactPhone);

        // Sauvegarder la réservation
        Booking savedBooking = bookingRepository.save(booking);

        // Mettre à jour le créneau
        slot.incrementBookings();

        // Ajouter l'utilisateur comme joueur principal
        addPlayerToBooking(savedBooking.getId(), userId, user.getFullName(), true);

        logger.info("Booking created with ID: {} and reference: {}",
                savedBooking.getId(), savedBooking.getBookingReference());

        return savedBooking;
    }

    /**
     * Confirme une réservation
     */
    public Booking confirmBooking(UUID bookingId) {
        logger.info("Confirming booking: {}", bookingId);

        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessValidationException("Only pending bookings can be confirmed");
        }

        if (booking.isExpired()) {
            throw new BusinessValidationException("Booking has expired and cannot be confirmed");
        }

        booking.confirm();
        booking.setStatus(BookingStatus.AWAITING_PAYMENT);

        Booking confirmedBooking = bookingRepository.save(booking);
        logger.info("Booking confirmed: {}", bookingId);

        return confirmedBooking;
    }

    /**
     * Annule une réservation
     */
    public Booking cancelBooking(UUID bookingId, String reason) {
        logger.info("Cancelling booking: {} with reason: {}", bookingId, reason);

        Booking booking = getBookingById(bookingId);

        if (!booking.canBeCancelled()) {
            throw new BusinessValidationException("Booking cannot be cancelled");
        }

        booking.cancel(reason);

        // Libérer la place dans le créneau
        Slot slot = booking.getSlot();
        slot.decrementBookings();

        Booking cancelledBooking = bookingRepository.save(booking);
        logger.info("Booking cancelled: {}", bookingId);

        return cancelledBooking;
    }

    /**
     * Ajoute un joueur à une réservation
     */
    public BookingPlayer addPlayerToBooking(UUID bookingId, UUID userId, String playerName, boolean isCaptain) {
        logger.info("Adding player {} to booking: {}", userId, bookingId);

        Booking booking = getBookingById(bookingId);
        User user = getUserById(userId);

        // Vérifications
        if (booking.isInFinalState()) {
            throw new BusinessValidationException("Cannot add players to finalized booking");
        }

        if (bookingPlayerRepository.existsByBookingIdAndUserId(bookingId, userId)) {
            throw new BusinessValidationException("User is already in this booking");
        }

        long currentPlayerCount = bookingPlayerRepository.countConfirmedPlayersByBooking(bookingId);
        if (currentPlayerCount >= booking.getNumberOfPlayers()) {
            throw new BusinessValidationException("Booking is already full");
        }

        BookingPlayer bookingPlayer = new BookingPlayer(booking, user, playerName);
        bookingPlayer.setIsCaptain(isCaptain);

        BookingPlayer savedPlayer = bookingPlayerRepository.save(bookingPlayer);
        logger.info("Player added to booking: {}", bookingId);

        return savedPlayer;
    }

    /**
     * Retire un joueur d'une réservation
     */
    public void removePlayerFromBooking(UUID bookingId, UUID userId) {
        logger.info("Removing player {} from booking: {}", userId, bookingId);

        Booking booking = getBookingById(bookingId);

        if (booking.isInFinalState()) {
            throw new BusinessValidationException("Cannot remove players from finalized booking");
        }

        List<BookingPlayer> players = bookingPlayerRepository.findByBookingIdAndStatus(bookingId, "CONFIRMED");
        BookingPlayer playerToRemove = players.stream()
                .filter(p -> p.getUser().getId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Player not found in this booking"));

        bookingPlayerRepository.delete(playerToRemove);
        logger.info("Player removed from booking: {}", bookingId);
    }

    /**
     * Marque une réservation comme payée
     */
    public Booking markBookingAsPaid(UUID bookingId) {
        logger.info("Marking booking as paid: {}", bookingId);

        Booking booking = getBookingById(bookingId);
        booking.markAsPaid();

        if (booking.getStatus() == BookingStatus.AWAITING_PAYMENT) {
            booking.setStatus(BookingStatus.CONFIRMED);
        }

        Booking paidBooking = bookingRepository.save(booking);
        logger.info("Booking marked as paid: {}", bookingId);

        return paidBooking;
    }

    /**
     * Complète une réservation (après le match)
     */
    public Booking completeBooking(UUID bookingId) {
        logger.info("Completing booking: {}", bookingId);

        Booking booking = getBookingById(bookingId);

        if (booking.getStatus() != BookingStatus.CONFIRMED) {
            throw new BusinessValidationException("Only confirmed bookings can be completed");
        }

        if (LocalDateTime.now().isBefore(booking.getSlot().getEndTime())) {
            throw new BusinessValidationException("Cannot complete booking before slot end time");
        }

        booking.complete();

        Booking completedBooking = bookingRepository.save(booking);
        logger.info("Booking completed: {}", bookingId);

        return completedBooking;
    }

    /**
     * Récupère une réservation par ID
     */
    @Transactional(readOnly = true)
    public Booking getBookingById(UUID bookingId) {
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", bookingId.toString()));
    }

    /**
     * Récupère une réservation par référence
     */
    @Transactional(readOnly = true)
    public Booking getBookingByReference(String bookingReference) {
        return bookingRepository.findByBookingReference(bookingReference)
                .orElseThrow(() -> new ResourceNotFoundException("Booking with reference", bookingReference));
    }

    /**
     * Récupère les réservations d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Booking> getUserBookings(UUID userId) {
        return bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * Récupère les réservations à venir d'un utilisateur
     */
    @Transactional(readOnly = true)
    public List<Booking> getUserUpcomingBookings(UUID userId) {
        List<BookingStatus> activeStatuses = List.of(BookingStatus.PENDING, BookingStatus.CONFIRMED, BookingStatus.AWAITING_PAYMENT);
        return bookingRepository.findUpcomingBookingsByUser(userId, LocalDateTime.now(), activeStatuses);
    }

    /**
     * Récupère l'historique des réservations d'un utilisateur
     */
    @Transactional(readOnly = true)
    public Page<Booking> getUserBookingHistory(UUID userId, Pageable pageable) {
        return bookingRepository.findUserBookingHistory(userId, LocalDateTime.now(), pageable);
    }

    /**
     * Recherche de réservations avec filtres
     */
    @Transactional(readOnly = true)
    public Page<Booking> searchBookings(UUID userId, BookingStatus status, BookingType bookingType,
                                        LocalDateTime startDate, LocalDateTime endDate,
                                        UUID establishmentId, Boolean isPaid, Pageable pageable) {
        return bookingRepository.findBookingsWithFilters(userId, status, bookingType, startDate,
                endDate, establishmentId, isPaid, pageable);
    }

    /**
     * Récupère les joueurs d'une réservation
     */
    @Transactional(readOnly = true)
    public List<BookingPlayer> getBookingPlayers(UUID bookingId) {
        return bookingPlayerRepository.findByBookingIdOrderByJoinedAt(bookingId);
    }

    /**
     * Nettoyage automatique des réservations expirées
     */
    @Scheduled(fixedRate = 300000) // Toutes les 5 minutes
    @Transactional
    public void cleanupExpiredBookings() {
        logger.info("Starting cleanup of expired bookings");

        LocalDateTime now = LocalDateTime.now();
        List<Booking> expiredBookings = bookingRepository.findExpiredPendingBookings(now);

        for (Booking booking : expiredBookings) {
            booking.setStatus(BookingStatus.EXPIRED);

            // Libérer la place dans le créneau
            Slot slot = booking.getSlot();
            slot.decrementBookings();

            bookingRepository.save(booking);
        }

        logger.info("Cleaned up {} expired bookings", expiredBookings.size());
    }

    /**
     * Calcule les statistiques d'un utilisateur
     */
    @Transactional(readOnly = true)
    public BookingStats getUserBookingStats(UUID userId) {
        Long completedBookings = bookingRepository.countCompletedBookingsByUser(userId);
        List<Booking> allBookings = bookingRepository.findByUserIdOrderByCreatedAtDesc(userId);

        long totalBookings = allBookings.size();
        long pendingBookings = allBookings.stream()
                .mapToLong(b -> b.getStatus() == BookingStatus.PENDING ? 1 : 0)
                .sum();
        long cancelledBookings = allBookings.stream()
                .mapToLong(b -> b.getStatus() == BookingStatus.CANCELLED ? 1 : 0)
                .sum();

        return new BookingStats(totalBookings, completedBookings, pendingBookings, cancelledBookings);
    }

    // Méthodes de validation privées

    private void validateBookingRequest(User user, Slot slot, BookingType bookingType, Integer numberOfPlayers) {
        if (!user.isEnabled()) {
            throw new BusinessValidationException("User account is disabled");
        }

        if (!slot.isAvailable()) {
            throw new BusinessValidationException("Slot is not available for booking");
        }

        if (slot.isInPast()) {
            throw new BusinessValidationException("Cannot book slots in the past");
        }

        if (numberOfPlayers == null || numberOfPlayers < 1) {
            throw new BusinessValidationException("Number of players must be at least 1");
        }

        if (numberOfPlayers > MAX_PLAYERS_PER_BOOKING) {
            throw new BusinessValidationException("Number of players exceeds maximum allowed");
        }

        if (bookingType == BookingType.TEAM && numberOfPlayers < 5) {
            throw new BusinessValidationException("Team bookings require at least 5 players");
        }
    }

    private void checkSlotAvailability(Slot slot, Integer numberOfPlayers) {
        if (slot.getAvailableSpots() < numberOfPlayers) {
            throw new BusinessValidationException("Not enough available spots in this slot");
        }
    }

    private void checkUserBookingLimits(UUID userId) {
        LocalDateTime today = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime tomorrow = today.plusDays(1);

        Long todayBookings = bookingRepository.countRecentBookingsByUser(userId, today);
        if (todayBookings >= MAX_BOOKINGS_PER_USER_PER_DAY) {
            throw new BusinessValidationException("Daily booking limit reached");
        }
    }

    private void checkDuplicateBooking(UUID userId, UUID slotId) {
        List<Booking> existingBookings = bookingRepository.findExistingBookingForUserAndSlot(userId, slotId);
        if (!existingBookings.isEmpty()) {
            throw new BusinessValidationException("User already has a booking for this slot");
        }
    }

    private BigDecimal calculateBookingAmount(Slot slot, Integer numberOfPlayers, BookingType bookingType) {
        BigDecimal basePrice = slot.getPrice();
        BigDecimal totalAmount = basePrice;

        // Ajustement selon le type de réservation
        if (bookingType == BookingType.PRIVATE_EVENT) {
            totalAmount = totalAmount.multiply(BigDecimal.valueOf(1.5)); // +50% pour événements privés
        } else if (slot.getIsPremium()) {
            totalAmount = totalAmount.multiply(BigDecimal.valueOf(1.2)); // +20% pour créneaux premium
        }

        // Réduction pour réservations individuelles avec peu de joueurs
        if (bookingType == BookingType.INDIVIDUAL && numberOfPlayers == 1) {
            totalAmount = totalAmount.multiply(BigDecimal.valueOf(0.5)); // -50% pour joueur seul
        }

        return totalAmount;
    }

    private User getUserById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId.toString()));
    }

    public static class BookingStats {
        private final long totalBookings;
        private final long completedBookings;
        private final long pendingBookings;
        private final long cancelledBookings;

        public BookingStats(long totalBookings, long completedBookings, long pendingBookings, long cancelledBookings) {
            this.totalBookings = totalBookings;
            this.completedBookings = completedBookings;
            this.pendingBookings = pendingBookings;
            this.cancelledBookings = cancelledBookings;
        }

        public long getTotalBookings() { return totalBookings; }
        public long getCompletedBookings() { return completedBookings; }
        public long getPendingBookings() { return pendingBookings; }
        public long getCancelledBookings() { return cancelledBookings; }

        public double getCompletionRate() {
            return totalBookings > 0 ? (double) completedBookings / totalBookings * 100 : 0;
        }

        public double getCancellationRate() {
            return totalBookings > 0 ? (double) cancelledBookings / totalBookings * 100 : 0;
        }
    }
}