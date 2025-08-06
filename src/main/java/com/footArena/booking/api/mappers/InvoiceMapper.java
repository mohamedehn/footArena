package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.InvoiceDTO;
import com.footArena.booking.domain.entities.Invoice;

public class InvoiceMapper {
    public static InvoiceDTO MappedInvoiceToDto(Invoice invoice) {
        InvoiceDTO invoiceDTO = new InvoiceDTO();
        invoiceDTO.setId(invoice.getId());
        invoiceDTO.setPaymentMethod(invoice.getPaymentMethod());
        invoiceDTO.setStatus(invoice.getStatus());
        invoiceDTO.setStripeSessionId(invoice.getStripeSessionId());
        invoiceDTO.setAmount(invoice.getAmount());
        invoiceDTO.setUserId(invoice.getUser().getId());
        invoiceDTO.setMatchId(invoice.getMatch().getId());
        invoiceDTO.setIssuedAt(invoice.getIssuedAt());
        return invoiceDTO;
    }

    public static Invoice MappedInvoiceToEntity(InvoiceDTO invoiceDTO) {
        Invoice invoice = new Invoice();
        invoice.setId(invoiceDTO.getId());
        invoice.setPaymentMethod(invoiceDTO.getPaymentMethod());
        invoice.setStatus(invoiceDTO.getStatus());
        invoice.setStripeSessionId(invoiceDTO.getStripeSessionId());
        invoice.setAmount(invoiceDTO.getAmount());
        // Assuming User and Match are mapped elsewhere -- TODO with service or repository
        invoice.setUser(null); // Placeholder, should be set with actual User entity
        invoice.setMatch(null); // Placeholder, should be set with actual Match entity
        invoice.setIssuedAt(invoiceDTO.getIssuedAt());
        return invoice;
    }

}