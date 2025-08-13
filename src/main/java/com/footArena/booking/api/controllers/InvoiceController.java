package com.footArena.booking.api.controllers;

import com.footArena.booking.api.dto.response.ApiResponse;
import com.footArena.booking.api.dto.response.InvoiceResponse;
import com.footArena.booking.api.mappers.InvoiceMapper;
import com.footArena.booking.domain.entities.Invoice;
import com.footArena.booking.domain.services.InvoiceService;
import com.footArena.booking.security.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/invoices")
@Tag(name = "Invoices", description = "Gestion des factures")
public class InvoiceController {

    private static final Logger logger = LoggerFactory.getLogger(InvoiceController.class);

    private final InvoiceService invoiceService;
    private final InvoiceMapper invoiceMapper;
    private final AuthService authService;

    public InvoiceController(InvoiceService invoiceService, InvoiceMapper invoiceMapper, AuthService authService) {
        this.invoiceService = invoiceService;
        this.invoiceMapper = invoiceMapper;
        this.authService = authService;
    }

    @Operation(summary = "Récupérer une facture par ID")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceById(
            @Parameter(description = "ID de la facture") @PathVariable UUID id) {

        logger.debug("Fetching invoice: {}", id);

        Invoice invoice = invoiceService.getInvoiceById(id);
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer une facture par numéro")
    @GetMapping("/number/{invoiceNumber}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getInvoiceByNumber(
            @Parameter(description = "Numéro de facture") @PathVariable String invoiceNumber) {

        logger.debug("Fetching invoice by number: {}", invoiceNumber);

        Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "Récupérer toutes les factures")
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getAllInvoices() {
        logger.debug("Fetching all invoices");

        List<Invoice> invoices = invoiceService.getAllInvoices();
        List<InvoiceResponse> responses = invoiceMapper.toResponseList(invoices);

        return ResponseEntity.ok(ApiResponse.success("Factures récupérées", responses));
    }

    @Operation(summary = "Récupérer les factures en retard")
    @GetMapping("/overdue")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> getOverdueInvoices() {
        logger.debug("Fetching overdue invoices");

        List<Invoice> invoices = invoiceService.getOverdueInvoices();
        List<InvoiceResponse> responses = invoiceMapper.toResponseList(invoices);

        return ResponseEntity.ok(ApiResponse.success("Factures en retard récupérées", responses));
    }

    @Operation(summary = "Marquer une facture comme payée")
    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> markInvoiceAsPaid(
            @Parameter(description = "ID de la facture") @PathVariable UUID id) {

        logger.info("Marking invoice as paid: {}", id);

        Invoice invoice = invoiceService.markInvoiceAsPaid(id);
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(ApiResponse.success("Facture marquée comme payée", response));
    }

    @Operation(summary = "Annuler une facture")
    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<ApiResponse<InvoiceResponse>> cancelInvoice(
            @Parameter(description = "ID de la facture") @PathVariable UUID id,
            @Parameter(description = "Raison de l'annulation") @RequestParam String reason) {

        logger.info("Cancelling invoice: {} for reason: {}", id, reason);

        Invoice invoice = invoiceService.cancelInvoice(id, reason);
        InvoiceResponse response = invoiceMapper.toResponse(invoice);

        return ResponseEntity.ok(ApiResponse.success("Facture annulée", response));
    }

    @Operation(summary = "Télécharger le PDF d'une facture")
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    public ResponseEntity<byte[]> downloadInvoicePdf(
            @Parameter(description = "ID de la facture") @PathVariable UUID id) {

        logger.info("Generating PDF for invoice: {}", id);

        try {
            Invoice invoice = invoiceService.getInvoiceById(id);
            byte[] pdfContent = invoiceService.generateInvoicePdf(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment",
                    "facture_" + invoice.getInvoiceNumber() + ".pdf");
            headers.setContentLength(pdfContent.length);

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(pdfContent);

        } catch (Exception e) {
            logger.error("Failed to generate PDF for invoice: {}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @Operation(summary = "Récupérer une facture publique (pour le client)")
    @GetMapping("/public/{invoiceNumber}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getPublicInvoice(
            @Parameter(description = "Numéro de facture") @PathVariable String invoiceNumber,
            HttpServletRequest httpRequest) {

        logger.debug("Fetching public invoice: {}", invoiceNumber);

        try {
            // Vérifier si l'utilisateur peut accéder à cette facture
            UUID userId = getCurrentUserId(httpRequest);
            Invoice invoice = invoiceService.getInvoiceByNumber(invoiceNumber);

            // Vérifier que la facture appartient à l'utilisateur connecté
            if (!invoice.getPayment().getBooking().getUser().getId().equals(userId)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(ApiResponse.error("Accès refusé à cette facture"));
            }

            InvoiceResponse response = invoiceMapper.toSimpleResponse(invoice);
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            logger.warn("Failed to fetch public invoice: {}", invoiceNumber, e);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error("Facture non trouvée"));
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