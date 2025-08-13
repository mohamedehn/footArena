package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Booking;
import com.footArena.booking.domain.entities.Invoice;
import com.footArena.booking.domain.entities.Payment;
import com.footArena.booking.domain.enums.PaymentMethod;
import com.footArena.booking.domain.enums.PaymentStatus;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.PaymentRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private static final Logger logger = LoggerFactory.getLogger(PaymentService.class);

    @Value("${stripe.secret-key:}")
    private String stripeSecretKey;

    @Value("${stripe.publishable-key:}")
    private String stripePublishableKey;

    private final PaymentRepository paymentRepository;
    private final BookingService bookingService;
    private final InvoiceService invoiceService;

    public PaymentService(PaymentRepository paymentRepository,
                          BookingService bookingService,
                          InvoiceService invoiceService) {
        this.paymentRepository = paymentRepository;
        this.bookingService = bookingService;
        this.invoiceService = invoiceService;
    }

    @PostConstruct
    public void init() {
        if (stripeSecretKey != null && !stripeSecretKey.isEmpty()) {
            Stripe.apiKey = stripeSecretKey;
            logger.info("Stripe initialized successfully");
        } else {
            logger.warn("Stripe secret key not configured - online payments will not be available");
        }
    }

    /**
     * Crée un paiement en espèces
     */
    public Payment createCashPayment(UUID bookingId, BigDecimal amount, String description) {
        logger.info("Creating cash payment for booking: {} amount: {}", bookingId, amount);

        Booking booking = bookingService.getBookingById(bookingId);
        validatePaymentRequest(booking, amount);

        Payment payment = new Payment(booking, amount, PaymentMethod.CASH);
        payment.setDescription(description);
        payment.markAsCompleted();

        Payment savedPayment = paymentRepository.save(payment);

        // Mettre à jour la réservation
        bookingService.markBookingAsPaid(bookingId);

        // Générer la facture
        invoiceService.generateInvoice(savedPayment);

        logger.info("Cash payment created with ID: {}", savedPayment.getId());
        return savedPayment;
    }

    /**
     * Crée une session de paiement Stripe
     */
    public String createStripeCheckoutSession(UUID bookingId, String successUrl, String cancelUrl) {
        logger.info("Creating Stripe checkout session for booking: {}", bookingId);

        Booking booking = bookingService.getBookingById(bookingId);
        validatePaymentRequest(booking, booking.getTotalAmount());

        try {
            // Créer la session Stripe
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl + "?session_id={CHECKOUT_SESSION_ID}")
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency("eur")
                                                    .setUnitAmount(booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue()) // Stripe utilise les centimes
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName("Réservation terrain de foot")
                                                                    .setDescription("Réservation " + booking.getBookingReference())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    )
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("bookingReference", booking.getBookingReference())
                    .build();

            Session session = Session.create(params);

            // Créer l'enregistrement de paiement
            Payment payment = new Payment(booking, booking.getTotalAmount(), PaymentMethod.STRIPE);
            payment.setStripeSessionId(session.getId());
            payment.setDescription("Paiement en ligne pour réservation " + booking.getBookingReference());
            payment.markAsProcessing();

            paymentRepository.save(payment);

            logger.info("Stripe session created: {}", session.getId());
            return session.getUrl();

        } catch (StripeException e) {
            logger.error("Failed to create Stripe session", e);
            throw new BusinessValidationException("Failed to initialize payment: " + e.getMessage());
        }
    }

    /**
     * Crée un Payment Intent Stripe (pour intégration personnalisée)
     */
    public String createStripePaymentIntent(UUID bookingId) {
        logger.info("Creating Stripe Payment Intent for booking: {}", bookingId);

        Booking booking = bookingService.getBookingById(bookingId);
        validatePaymentRequest(booking, booking.getTotalAmount());

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(booking.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue())
                    .setCurrency("eur")
                    .setDescription("Réservation " + booking.getBookingReference())
                    .putMetadata("bookingId", bookingId.toString())
                    .putMetadata("bookingReference", booking.getBookingReference())
                    .build();

            PaymentIntent paymentIntent = PaymentIntent.create(params);

            // Créer l'enregistrement de paiement
            Payment payment = new Payment(booking, booking.getTotalAmount(), PaymentMethod.STRIPE);
            payment.setStripePaymentIntentId(paymentIntent.getId());
            payment.setDescription("Paiement en ligne pour réservation " + booking.getBookingReference());

            paymentRepository.save(payment);

            logger.info("Payment Intent created: {}", paymentIntent.getId());
            return paymentIntent.getClientSecret();

        } catch (StripeException e) {
            logger.error("Failed to create Payment Intent", e);
            throw new BusinessValidationException("Failed to initialize payment: " + e.getMessage());
        }
    }

    /**
     * Confirme un paiement Stripe
     */
    public Payment confirmStripePayment(String stripeSessionId) {
        logger.info("Confirming Stripe payment for session: {}", stripeSessionId);

        Payment payment = paymentRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for session", stripeSessionId));

        try {
            Session session = Session.retrieve(stripeSessionId);

            if ("complete".equals(session.getStatus()) && "paid".equals(session.getPaymentStatus())) {
                payment.markAsCompleted();

                // Mettre à jour la réservation
                bookingService.markBookingAsPaid(payment.getBooking().getId());

                // Générer la facture
                invoiceService.generateInvoice(payment);

                logger.info("Stripe payment confirmed: {}", payment.getId());
            } else {
                payment.markAsFailed("Payment not completed in Stripe");
                logger.warn("Stripe payment failed for session: {}", stripeSessionId);
            }

            return paymentRepository.save(payment);

        } catch (StripeException e) {
            logger.error("Failed to confirm Stripe payment", e);
            payment.markAsFailed("Stripe error: " + e.getMessage());
            return paymentRepository.save(payment);
        }
    }

    /**
     * Traite un remboursement
     */
    public Payment refundPayment(UUID paymentId, BigDecimal refundAmount, String reason) {
        logger.info("Processing refund for payment: {} amount: {}", paymentId, refundAmount);

        Payment payment = getPaymentById(paymentId);

        if (!payment.canBeRefunded()) {
            throw new BusinessValidationException("Payment cannot be refunded");
        }

        if (refundAmount.compareTo(payment.getRemainingRefundableAmount()) > 0) {
            throw new BusinessValidationException("Refund amount exceeds remaining refundable amount");
        }

        // Si c'est un paiement Stripe, traiter le remboursement via Stripe
        if (payment.getPaymentMethod() == PaymentMethod.STRIPE && payment.getStripePaymentIntentId() != null) {
            processStripeRefund(payment, refundAmount, reason);
        }

        // Mettre à jour le paiement
        payment.refund(refundAmount, reason);
        Payment refundedPayment = paymentRepository.save(payment);

        logger.info("Refund processed successfully: {}", paymentId);
        return refundedPayment;
    }

    /**
     * Récupère un paiement par ID
     */
    @Transactional(readOnly = true)
    public Payment getPaymentById(UUID paymentId) {
        return paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment", paymentId.toString()));
    }

    /**
     * Récupère les paiements d'une réservation
     */
    @Transactional(readOnly = true)
    public List<Payment> getBookingPayments(UUID bookingId) {
        return paymentRepository.findByBookingIdOrderByCreatedAtDesc(bookingId);
    }

    /**
     * Calcule les revenus pour une période
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        BigDecimal revenue = paymentRepository.calculateRevenueByPeriod(startDate, endDate);
        return revenue != null ? revenue : BigDecimal.ZERO;
    }

    /**
     * Récupère les statistiques de revenus par méthode de paiement
     */
    @Transactional(readOnly = true)
    public Map<PaymentMethod, BigDecimal> getRevenueByPaymentMethod(LocalDateTime startDate, LocalDateTime endDate) {
        List<Object[]> results = paymentRepository.findRevenueByPaymentMethod(startDate, endDate);
        Map<PaymentMethod, BigDecimal> revenueMap = new HashMap<>();

        for (Object[] result : results) {
            PaymentMethod method = (PaymentMethod) result[0];
            BigDecimal amount = (BigDecimal) result[1];
            revenueMap.put(method, amount);
        }

        return revenueMap;
    }

    /**
     * Nettoyage automatique des paiements expirés
     */
    @Scheduled(fixedRate = 600000) // Toutes les 10 minutes
    @Transactional
    public void cleanupExpiredPayments() {
        logger.info("Starting cleanup of expired payments");

        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(2);
        List<Payment> expiredPayments = paymentRepository.findExpiredPayments(cutoffTime);

        for (Payment payment : expiredPayments) {
            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);
        }

        logger.info("Cleaned up {} expired payments", expiredPayments.size());
    }

    // Méthodes privées

    private void validatePaymentRequest(Booking booking, BigDecimal amount) {
        if (booking.getIsPaid()) {
            throw new BusinessValidationException("Booking is already paid");
        }

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessValidationException("Payment amount must be positive");
        }

        if (amount.compareTo(booking.getRemainingAmount()) > 0) {
            throw new BusinessValidationException("Payment amount exceeds remaining amount");
        }
    }

    private void processStripeRefund(Payment payment, BigDecimal refundAmount, String reason) {
        try {
            // Ici on traiterait le remboursement via l'API Stripe
            // com.stripe.model.Refund.create(params);
            logger.info("Stripe refund would be processed here for payment: {}", payment.getId());
        } catch (Exception e) {
            logger.error("Failed to process Stripe refund", e);
            throw new BusinessValidationException("Failed to process refund via Stripe: " + e.getMessage());
        }
    }
}