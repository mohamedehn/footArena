package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.request.CreateBookingRequest;
import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.BookingPlayerResponse;
import com.footArena.booking.api.dto.response.BookingResponse;
import com.footArena.booking.api.dto.response.PageResponse;
import com.footArena.booking.api.mappers.BookingMapper;
import com.footArena.booking.domain.entities.Booking;
import com.footArena.booking.domain.entities.BookingPlayer;
import com.footArena.booking.domain.enums.BookingStatus;
import com.footArena.booking.domain.enums.BookingType;
import com.footArena.booking.domain.services.BookingService;
import com.footArena.booking.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/bookings")
@Tag(name = "Bookings", description = "Gestion des réservations")
public class BookingController {

    private static final Logger logger = LoggerFactory.getLogger(BookingController.class);

    private final BookingService bookingService;
    private final BookingMapper bookingMapper;
    private final AuthService authService;

    public BookingController(BookingService bookingService, BookingMapper bookingMapper, AuthService authService) {
        this.bookingService = bookingService;
        this.bookingMapper = bookingMapper;
        this.authService = authService;
    }

    @Operation(summary = "Créer une nouvelle réservation",
            description = "Crée une nouvelle réservation pour un créneau")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Réservation créée avec succès"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Données invalides"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Accès refusé")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody CreateBookingRequest request,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Creating booking for user: {} and slot: {}", userId, request.getSlotId());

        Booking booking = bookingService.createBooking(
                userId,
                request.getSlotId(),
                request.getBookingType(),
                request.getNumberOfPlayers(),
                request.getTeamName(),
                request.getSpecialRequests(),
                request.getContactPhone()
        );

        BookingResponse response = bookingMapper.toResponse(booking);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Réservation créée avec succès", response));
    }

    @Operation(summary = "Récupérer une réservation par ID")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching booking: {} for user: {}", id, userId);

        Booking booking = bookingService.getBookingById(id);

        // Vérifier que l'utilisateur peut accéder à cette réservation
        if (!canAccessBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Accès refusé à cette réservation"));
        }

        BookingResponse response = bookingMapper.toResponse(booking);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer une réservation par référence")
    @GetMapping("/reference/{reference}")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingByReference(
            @Parameter(description = "Référence de la réservation") @PathVariable String reference,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching booking by reference: {} for user: {}", reference, userId);

        Booking booking = bookingService.getBookingByReference(reference);

        if (!canAccessBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Accès refusé à cette réservation"));
        }

        BookingResponse response = bookingMapper.toResponse(booking);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer les réservations de l'utilisateur connecté")
    @GetMapping("/my-bookings")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyBookings(
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching bookings for user: {}", userId);

        List<Booking> bookings = bookingService.getUserBookings(userId);
        List<BookingResponse> responses = bookingMapper.toResponseList(bookings);

        return ResponseEntity.ok(ApiResponse.success("Réservations récupérées", responses));
    }

    @Operation(summary = "Récupérer les réservations à venir de l'utilisateur")
    @GetMapping("/my-bookings/upcoming")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getMyUpcomingBookings(
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching upcoming bookings for user: {}", userId);

        List<Booking> bookings = bookingService.getUserUpcomingBookings(userId);
        List<BookingResponse> responses = bookingMapper.toResponseList(bookings);

        return ResponseEntity.ok(ApiResponse.success("Réservations à venir récupérées", responses));
    }

    @Operation(summary = "Récupérer l'historique des réservations")
    @GetMapping("/my-bookings/history")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookingHistory(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching booking history for user: {}", userId);

        Sort sort = Sort.by("slot.startTime").descending();
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Booking> bookings = bookingService.getUserBookingHistory(userId, pageable);
        Page<BookingResponse> responsePage = bookings.map(bookingMapper::toSimpleResponse);
        PageResponse<BookingResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Historique récupéré", pageResponse));
    }

    @Operation(summary = "Rechercher des réservations avec filtres")
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> searchBookings(
            @Parameter(description = "ID utilisateur") @RequestParam(required = false) UUID userId,
            @Parameter(description = "Statut de la réservation") @RequestParam(required = false) BookingStatus status,
            @Parameter(description = "Type de réservation") @RequestParam(required = false) BookingType bookingType,
            @Parameter(description = "Date de début") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @Parameter(description = "Date de fin") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            @Parameter(description = "ID établissement") @RequestParam(required = false) UUID establishmentId,
            @Parameter(description = "Payé") @RequestParam(required = false) Boolean isPaid,
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Searching bookings with filters");

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Booking> bookings = bookingService.searchBookings(userId, status, bookingType,
                startDate, endDate, establishmentId, isPaid, pageable);

        Page<BookingResponse> responsePage = bookings.map(bookingMapper::toSimpleResponse);
        PageResponse<BookingResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Recherche effectuée", pageResponse));
    }

    @Operation(summary = "Confirmer une réservation")
    @PostMapping("/{id}/confirm")
    public ResponseEntity<ApiResponse<BookingResponse>> confirmBooking(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Confirming booking: {} by user: {}", id, userId);

        Booking booking = bookingService.getBookingById(id);

        if (!canModifyBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Vous ne pouvez pas confirmer cette réservation"));
        }

        Booking confirmedBooking = bookingService.confirmBooking(id);
        BookingResponse response = bookingMapper.toResponse(confirmedBooking);

        return ResponseEntity.ok(ApiResponse.success("Réservation confirmée avec succès", response));
    }

    @Operation(summary = "Annuler une réservation")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            @Parameter(description = "Raison de l'annulation") @RequestParam(required = false) String reason,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Cancelling booking: {} by user: {}", id, userId);

        Booking booking = bookingService.getBookingById(id);

        if (!canModifyBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Vous ne pouvez pas annuler cette réservation"));
        }

        Booking cancelledBooking = bookingService.cancelBooking(id, reason);
        BookingResponse response = bookingMapper.toResponse(cancelledBooking);

        return ResponseEntity.ok(ApiResponse.success("Réservation annulée avec succès", response));
    }

    @Operation(summary = "Ajouter un joueur à une réservation")
    @PostMapping("/{id}/players")
    public ResponseEntity<ApiResponse<BookingPlayerResponse>> addPlayerToBooking(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            @Parameter(description = "ID du joueur") @RequestParam UUID playerId,
            @Parameter(description = "Nom d'affichage") @RequestParam String playerName,
            @Parameter(description = "Est capitaine") @RequestParam(defaultValue = "false") boolean isCaptain,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Adding player {} to booking: {}", playerId, id);

        Booking booking = bookingService.getBookingById(id);

        if (!canModifyBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Vous ne pouvez pas modifier cette réservation"));
        }

        BookingPlayer bookingPlayer = bookingService.addPlayerToBooking(id, playerId, playerName, isCaptain);
        BookingPlayerResponse response = bookingMapper.toBookingPlayerResponse(bookingPlayer);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Joueur ajouté à la réservation", response));
    }

    @Operation(summary = "Retirer un joueur d'une réservation")
    @DeleteMapping("/{id}/players/{playerId}")
    public ResponseEntity<ApiResponse<Void>> removePlayerFromBooking(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            @Parameter(description = "ID du joueur") @PathVariable UUID playerId,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Removing player {} from booking: {}", playerId, id);

        Booking booking = bookingService.getBookingById(id);

        if (!canModifyBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Vous ne pouvez pas modifier cette réservation"));
        }

        bookingService.removePlayerFromBooking(id, playerId);

        return ResponseEntity.ok(ApiResponse.success("Joueur retiré de la réservation"));
    }

    @Operation(summary = "Récupérer les joueurs d'une réservation")
    @GetMapping("/{id}/players")
    public ResponseEntity<ApiResponse<List<BookingPlayerResponse>>> getBookingPlayers(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching players for booking: {}", id);

        Booking booking = bookingService.getBookingById(id);

        if (!canAccessBooking(booking, userId, httpRequest)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.error("Accès refusé à cette réservation"));
        }

        List<BookingPlayer> players = bookingService.getBookingPlayers(id);
        List<BookingPlayerResponse> responses = bookingMapper.toBookingPlayerResponseList(players);

        return ResponseEntity.ok(ApiResponse.success("Joueurs récupérés", responses));
    }

    @Operation(summary = "Compléter une réservation")
    @PostMapping("/{id}/complete")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BookingResponse>> completeBooking(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id) {

        logger.info("Completing booking: {}", id);

        Booking completedBooking = bookingService.completeBooking(id);
        BookingResponse response = bookingMapper.toResponse(completedBooking);

        return ResponseEntity.ok(ApiResponse.success("Réservation complétée", response));
    }

    @Operation(summary = "Récupérer les statistiques des réservations")
    @GetMapping("/stats/user")
    public ResponseEntity<ApiResponse<BookingService.BookingStats>> getUserBookingStats(
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching booking stats for user: {}", userId);

        BookingService.BookingStats stats = bookingService.getUserBookingStats(userId);

        return ResponseEntity.ok(ApiResponse.success("Statistiques récupérées", stats));
    }

    @Operation(summary = "Marquer une réservation comme payée")
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BookingResponse>> markBookingAsPaid(
            @Parameter(description = "ID de la réservation") @PathVariable UUID id) {

        logger.info("Marking booking as paid: {}", id);

        Booking paidBooking = bookingService.markBookingAsPaid(id);
        BookingResponse response = bookingMapper.toResponse(paidBooking);

        return ResponseEntity.ok(ApiResponse.success("Réservation marquée comme payée", response));
    }

    @Operation(summary = "Récupérer toutes les réservations (Admin/Manager)")
    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getAllBookings(
            @Parameter(description = "Numéro de page") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Taille de page") @RequestParam(defaultValue = "20") int size,
            @Parameter(description = "Champ de tri") @RequestParam(defaultValue = "createdAt") String sortBy,
            @Parameter(description = "Direction du tri") @RequestParam(defaultValue = "desc") String sortDir) {

        logger.debug("Fetching all bookings - page: {}, size: {}", page, size);

        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Booking> bookings = bookingService.searchBookings(null, null, null,
                null, null, null, null, pageable);

        Page<BookingResponse> responsePage = bookings.map(bookingMapper::toSimpleResponse);
        PageResponse<BookingResponse> pageResponse = PageResponse.of(responsePage);

        return ResponseEntity.ok(ApiResponse.success("Toutes les réservations récupérées", pageResponse));
    }

    @Operation(summary = "Récupérer les réservations du jour")
    @GetMapping("/today")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getTodayBookings() {

        logger.debug("Fetching today's bookings");

        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        // Utilisation de la méthode searchBookings avec des dates pour aujourd'hui
        Pageable pageable = PageRequest.of(0, 100); // Grande taille pour récupérer toutes les réservations du jour
        Page<Booking> bookings = bookingService.searchBookings(null, null, null,
                startOfDay, endOfDay, null, null, pageable);

        List<BookingResponse> responses = bookingMapper.toResponseList(bookings.getContent());

        return ResponseEntity.ok(ApiResponse.success("Réservations du jour récupérées", responses));
    }

    // Méthodes utilitaires privées

    private UUID getCurrentUserId(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return authService.getUserFromToken(token).getId();
    }

    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        throw new IllegalArgumentException("No valid token found in request");
    }

    private boolean canAccessBooking(Booking booking, UUID userId, HttpServletRequest request) {
        // L'utilisateur peut accéder à ses propres réservations
        if (booking.getUser().getId().equals(userId)) {
            return true;
        }

        // Les admins et managers peuvent accéder à toutes les réservations
        String token = extractTokenFromRequest(request);
        try {
            String role = authService.getUserFromToken(token).getRole().name();
            return "ADMIN".equals(role) || "MANAGER".equals(role);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canModifyBooking(Booking booking, UUID userId, HttpServletRequest request) {
        // L'utilisateur peut modifier ses propres réservations
        if (booking.getUser().getId().equals(userId)) {
            return true;
        }

        // Les admins et managers peuvent modifier toutes les réservations
        String token = extractTokenFromRequest(request);
        try {
            String role = authService.getUserFromToken(token).getRole().name();
            return "ADMIN".equals(role) || "MANAGER".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}