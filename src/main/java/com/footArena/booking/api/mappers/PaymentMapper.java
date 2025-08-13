package com.footArena.booking.api.mappers;

import com.footArena.booking.api.dto.response.PaymentResponse;
import com.footArena.booking.domain.entities.Payment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PaymentMapper {

    /**
     * Convertit une entité Payment en PaymentResponse
     */
    public PaymentResponse toResponse(Payment payment) {
        if (payment == null) {
            return null;
        }

        PaymentResponse response = new PaymentResponse();
        response.setId(payment.getId());
        response.setAmount(payment.getAmount());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setStatus(payment.getStatus());
        response.setTransactionReference(payment.getTransactionReference());
        response.setCurrency(payment.getCurrency());
        response.setDescription(payment.getDescription());
        response.setFailureReason(payment.getFailureReason());
        response.setProcessedAt(payment.getProcessedAt());
        response.setRefundAmount(payment.getRefundAmount());
        response.setRefundReason(payment.getRefundReason());
        response.setRefundedAt(payment.getRefundedAt());
        response.setCreatedAt(payment.getCreatedAt());

        return response;
    }

    /**
     * Convertit une liste d'entités Payment en liste de PaymentResponse
     */
    public List<PaymentResponse> toResponseList(List<Payment> payments) {
        if (payments == null) {
            return List.of();
        }

        return payments.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }
}