package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.PaymentResponse;
import com.footArena.booking.api.mappers.PaymentMapper;
import com.footArena.booking.domain.entities.Payment;
import com.footArena.booking.domain.enums.PaymentMethod;
import com.footArena.booking.domain.services.PaymentService;
import com.footArena.booking.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Payments", description = "Gestion des paiements")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final AuthService authService;

    public PaymentController(PaymentService paymentService, PaymentMapper paymentMapper, AuthService authService) {
        this.paymentService = paymentService;
        this.paymentMapper = paymentMapper;
        this.authService = authService;
    }

    @Operation(summary = "Créer un paiement en espèces",
            description = "Enregistre un paiement en espèces pour une réservation")
    @PostMapping("/cash")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> createCashPayment(
            @Parameter(description = "ID de la réservation") @RequestParam @NotNull UUID bookingId,
            @Parameter(description = "Montant du paiement") @RequestParam @NotNull @DecimalMin("0.01") BigDecimal amount,
            @Parameter(description = "Description") @RequestParam(required = false) String description) {

        logger.info("Creating cash payment for booking: {} amount: {}", bookingId, amount);

        Payment payment = paymentService.createCashPayment(bookingId, amount, description);
        PaymentResponse response = paymentMapper.toResponse(payment);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Paiement en espèces enregistré", response));
    }

    @Operation(summary = "Créer une session de paiement Stripe",
            description = "Crée une session Stripe Checkout pour un paiement en ligne")
    @PostMapping("/stripe/create-checkout-session")
    public ResponseEntity<ApiResponse<String>> createStripeCheckoutSession(
            @Parameter(description = "ID de la réservation") @RequestParam @NotNull UUID bookingId,
            @Parameter(description = "URL de succès") @RequestParam @NotNull String successUrl,
            @Parameter(description = "URL d'annulation") @RequestParam @NotNull String cancelUrl,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Creating Stripe session for booking: {} by user: {}", bookingId, userId);

        try {
            String checkoutUrl = paymentService.createStripeCheckoutSession(bookingId, successUrl, cancelUrl);

            return ResponseEntity.ok(ApiResponse.success("Session Stripe créée", checkoutUrl));

        } catch (Exception e) {
            logger.error("Failed to create Stripe session", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la création de la session: " + e.getMessage()));
        }
    }

    @Operation(summary = "Créer un Payment Intent Stripe",
            description = "Crée un Payment Intent pour intégration Stripe personnalisée")
    @PostMapping("/stripe/create-payment-intent")
    public ResponseEntity<ApiResponse<String>> createStripePaymentIntent(
            @Parameter(description = "ID de la réservation") @RequestParam @NotNull UUID bookingId,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.info("Creating Payment Intent for booking: {} by user: {}", bookingId, userId);

        try {
            String clientSecret = paymentService.createStripePaymentIntent(bookingId);

            return ResponseEntity.ok(ApiResponse.success("Payment Intent créé", clientSecret));

        } catch (Exception e) {
            logger.error("Failed to create Payment Intent", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la création du Payment Intent: " + e.getMessage()));
        }
    }

    @Operation(summary = "Confirmer un paiement Stripe",
            description = "Confirme un paiement après succès de Stripe Checkout")
    @PostMapping("/stripe/confirm")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmStripePayment(
            @Parameter(description = "ID de session Stripe") @RequestParam @NotNull String sessionId) {

        logger.info("Confirming Stripe payment for session: {}", sessionId);

        try {
            Payment payment = paymentService.confirmStripePayment(sessionId);
            PaymentResponse response = paymentMapper.toResponse(payment);

            return ResponseEntity.ok(ApiResponse.success("Paiement confirmé avec succès", response));

        } catch (Exception e) {
            logger.error("Failed to confirm Stripe payment", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors de la confirmation: " + e.getMessage()));
        }
    }

    @Operation(summary = "Récupérer un paiement par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> getPaymentById(
            @Parameter(description = "ID du paiement") @PathVariable UUID id) {

        logger.debug("Fetching payment: {}", id);

        Payment payment = paymentService.getPaymentById(id);
        PaymentResponse response = paymentMapper.toResponse(payment);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer les paiements d'une réservation")
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<ApiResponse<List<PaymentResponse>>> getBookingPayments(
            @Parameter(description = "ID de la réservation") @PathVariable UUID bookingId,
            HttpServletRequest httpRequest) {

        UUID userId = getCurrentUserId(httpRequest);
        logger.debug("Fetching payments for booking: {} by user: {}", bookingId, userId);

        List<Payment> payments = paymentService.getBookingPayments(bookingId);
        List<PaymentResponse> responses = paymentMapper.toResponseList(payments);

        return ResponseEntity.ok(ApiResponse.success("Paiements récupérés", responses));
    }

    @Operation(summary = "Traiter un remboursement")
    @PostMapping("/{id}/refund")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<PaymentResponse>> refundPayment(
            @Parameter(description = "ID du paiement") @PathVariable UUID id,
            @Parameter(description = "Montant à rembourser") @RequestParam @NotNull @DecimalMin("0.01") BigDecimal refundAmount,
            @Parameter(description = "Raison du remboursement") @RequestParam @NotNull String reason) {

        logger.info("Processing refund for payment: {} amount: {}", id, refundAmount);

        try {
            Payment payment = paymentService.refundPayment(id, refundAmount, reason);
            PaymentResponse response = paymentMapper.toResponse(payment);

            return ResponseEntity.ok(ApiResponse.success("Remboursement traité avec succès", response));

        } catch (Exception e) {
            logger.error("Failed to process refund", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.error("Erreur lors du remboursement: " + e.getMessage()));
        }
    }

    @Operation(summary = "Calculer les revenus pour une période")
    @GetMapping("/revenue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<BigDecimal>> calculateRevenue(
            @Parameter(description = "Date de début") @RequestParam String startDate,
            @Parameter(description = "Date de fin") @RequestParam String endDate) {

        logger.debug("Calculating revenue from {} to {}", startDate, endDate);

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        BigDecimal revenue = paymentService.calculateRevenue(start, end);

        return ResponseEntity.ok(ApiResponse.success("Revenus calculés", revenue));
    }

    @Operation(summary = "Statistiques des revenus par méthode de paiement")
    @GetMapping("/revenue/by-method")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<Map<PaymentMethod, BigDecimal>>> getRevenueByPaymentMethod(
            @Parameter(description = "Date de début") @RequestParam String startDate,
            @Parameter(description = "Date de fin") @RequestParam String endDate) {

        logger.debug("Getting revenue by payment method from {} to {}", startDate, endDate);

        LocalDateTime start = LocalDateTime.parse(startDate);
        LocalDateTime end = LocalDateTime.parse(endDate);

        Map<PaymentMethod, BigDecimal> revenueByMethod = paymentService.getRevenueByPaymentMethod(start, end);

        return ResponseEntity.ok(ApiResponse.success("Statistiques des revenus", revenueByMethod));
    }

    @Operation(summary = "Webhook Stripe",
            description = "Endpoint pour recevoir les webhooks de Stripe")
    @PostMapping("/stripe/webhook")
    public ResponseEntity<String> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        logger.info("Received Stripe webhook");

        try {
            // TODO: Implémenter la vérification de signature et le traitement des événements Stripe
            // Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

            logger.info("Stripe webhook processed successfully");
            return ResponseEntity.ok("Webhook handled");

        } catch (Exception e) {
            logger.error("Failed to process Stripe webhook", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Webhook failed");
        }
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
}