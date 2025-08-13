package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.InvoiceResponse;
import com.footArena.booking.domain.entities.Invoice;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class InvoiceMapper {

    private final PaymentMapper paymentMapper;

    public InvoiceMapper(PaymentMapper paymentMapper) {
        this.paymentMapper = paymentMapper;
    }

    /**
     * Convertit une entité Invoice en InvoiceResponse
     */
    public InvoiceResponse toResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setAmountHT(invoice.getAmountHT());
        response.setTaxRate(invoice.getTaxRate());
        response.setTaxAmount(invoice.getTaxAmount());
        response.setAmountTTC(invoice.getAmountTTC());
        response.setStatus(invoice.getStatus());
        response.setDueDate(invoice.getDueDate());
        response.setPaidAt(invoice.getPaidAt());
        response.setDescription(invoice.getDescription());
        response.setCustomerName(invoice.getCustomerName());
        response.setCustomerEmail(invoice.getCustomerEmail());
        response.setCustomerAddress(invoice.getCustomerAddress());
        response.setEstablishmentName(invoice.getEstablishmentName());
        response.setEstablishmentAddress(invoice.getEstablishmentAddress());
        response.setEstablishmentSiret(invoice.getEstablishmentSiret());
        response.setInvoiceFilePath(invoice.getInvoiceFilePath());
        response.setIssuedAt(invoice.getIssuedAt());
        response.setIsPaid(invoice.isPaid());
        response.setIsCancelled(invoice.isCancelled());
        response.setIsOverdue(invoice.isOverdue());

        // Ajouter les informations du paiement associé
        if (invoice.getPayment() != null) {
            response.setPayment(paymentMapper.toResponse(invoice.getPayment()));
        }

        return response;
    }

    /**
     * Convertit une entité Invoice en InvoiceResponse simplifié (sans relations)
     */
    public InvoiceResponse toSimpleResponse(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceNumber(invoice.getInvoiceNumber());
        response.setAmountTTC(invoice.getAmountTTC());
        response.setStatus(invoice.getStatus());
        response.setDueDate(invoice.getDueDate());
        response.setPaidAt(invoice.getPaidAt());
        response.setCustomerName(invoice.getCustomerName());
        response.setCustomerEmail(invoice.getCustomerEmail());
        response.setEstablishmentName(invoice.getEstablishmentName());
        response.setIssuedAt(invoice.getIssuedAt());
        response.setIsPaid(invoice.isPaid());
        response.setIsCancelled(invoice.isCancelled());
        response.setIsOverdue(invoice.isOverdue());

        return response;
    }

    /**
     * Convertit une liste d'entités Invoice en liste de InvoiceResponse
     */
    public List<InvoiceResponse> toResponseList(List<Invoice> invoices) {
        if (invoices == null) {
            return List.of();
        }

        return invoices.stream()
                .map(this::toSimpleResponse) // Utilisation de la version simplifiée pour les listes
                .collect(Collectors.toList());
    }
}