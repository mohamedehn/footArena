package com.footArena.booking.domain.services;

import com.footArena.booking.domain.entities.Invoice;
import com.footArena.booking.domain.entities.Payment;
import com.footArena.booking.domain.exceptions.BusinessValidationException;
import com.footArena.booking.domain.exceptions.ResourceNotFoundException;
import com.footArena.booking.domain.repositories.InvoiceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceService.class);

    private final InvoiceRepository invoiceRepository;

    public InvoiceService(InvoiceRepository invoiceRepository) {
        this.invoiceRepository = invoiceRepository;
    }

    /**
     * Génère une facture pour un paiement
     */
    public Invoice generateInvoice(Payment payment) {
        logger.info("Generating invoice for payment: {}", payment.getId());

        if (!payment.isCompleted()) {
            throw new BusinessValidationException("Cannot generate invoice for incomplete payment");
        }

        // Vérifier si une facture existe déjà
        if (payment.getInvoice() != null) {
            logger.warn("Invoice already exists for payment: {}", payment.getId());
            return payment.getInvoice();
        }

        // Extraire les informations client et établissement
        String customerName = payment.getBooking().getUser().getFullName();
        String customerEmail = payment.getBooking().getUser().getEmail();
        String establishmentName = payment.getBooking().getSlot().getField().getEstablishment().getName();
        String establishmentAddress = payment.getBooking().getSlot().getField().getEstablishment().getAddress();

        // Créer la facture
        Invoice invoice = new Invoice(payment, customerName, customerEmail, establishmentName, establishmentAddress);

        // Définir la description détaillée
        String description = String.format(
                "Réservation terrain de football\n" +
                        "Terrain: %s\n" +
                        "Date: %s\n" +
                        "Horaire: %s - %s\n" +
                        "Référence: %s",
                payment.getBooking().getSlot().getField().getName(),
                payment.getBooking().getSlot().getStartTime().toLocalDate(),
                payment.getBooking().getSlot().getStartTime().toLocalTime(),
                payment.getBooking().getSlot().getEndTime().toLocalTime(),
                payment.getBooking().getBookingReference()
        );
        invoice.setDescription(description);

        Invoice savedInvoice = invoiceRepository.save(invoice);

        // Marquer comme payée si le paiement est terminé
        if (payment.isCompleted()) {
            savedInvoice.markAsPaid();
            invoiceRepository.save(savedInvoice);
        }

        logger.info("Invoice generated with number: {}", savedInvoice.getInvoiceNumber());
        return savedInvoice;
    }

    /**
     * Récupère une facture par ID
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceById(UUID invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice", invoiceId.toString()));
    }

    /**
     * Récupère une facture par numéro
     */
    @Transactional(readOnly = true)
    public Invoice getInvoiceByNumber(String invoiceNumber) {
        return invoiceRepository.findByInvoiceNumber(invoiceNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice with number", invoiceNumber));
    }

    /**
     * Récupère toutes les factures
     */
    @Transactional(readOnly = true)
    public List<Invoice> getAllInvoices() {
        return invoiceRepository.findAll();
    }

    /**
     * Annule une facture
     */
    public Invoice cancelInvoice(UUID invoiceId, String reason) {
        logger.info("Cancelling invoice: {} for reason: {}", invoiceId, reason);

        Invoice invoice = getInvoiceById(invoiceId);

        if (invoice.isPaid()) {
            throw new BusinessValidationException("Cannot cancel paid invoice");
        }

        invoice.cancel();
        Invoice cancelledInvoice = invoiceRepository.save(invoice);

        logger.info("Invoice cancelled: {}", invoiceId);
        return cancelledInvoice;
    }

    /**
     * Marque une facture comme payée
     */
    public Invoice markInvoiceAsPaid(UUID invoiceId) {
        logger.info("Marking invoice as paid: {}", invoiceId);

        Invoice invoice = getInvoiceById(invoiceId);
        invoice.markAsPaid();

        Invoice paidInvoice = invoiceRepository.save(invoice);
        logger.info("Invoice marked as paid: {}", invoiceId);

        return paidInvoice;
    }

    /**
     * Récupère les factures en retard
     */
    @Transactional(readOnly = true)
    public List<Invoice> getOverdueInvoices() {
        return invoiceRepository.findAll().stream()
                .filter(Invoice::isOverdue)
                .toList();
    }

    /**
     * Génère le PDF d'une facture (méthode placeholder)
     */
    public byte[] generateInvoicePdf(UUID invoiceId) {
        logger.info("Generating PDF for invoice: {}", invoiceId);

        Invoice invoice = getInvoiceById(invoiceId);

        // TODO: Implémenter la génération PDF avec une librairie comme iText ou Apache PDFBox
        // Ici on retournerait le byte array du PDF généré

        logger.info("PDF generated for invoice: {}", invoice.getInvoiceNumber());
        return new byte[0]; // Placeholder
    }
}