package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    // Recherche par numéro de facture
    Optional<Invoice> findByInvoiceNumber(String invoiceNumber);

    // Factures par statut
    List<Invoice> findByStatus(String status);

    // Factures par client
    List<Invoice> findByCustomerEmailOrderByIssuedAtDesc(String customerEmail);

    // Factures par établissement
    List<Invoice> findByEstablishmentNameOrderByIssuedAtDesc(String establishmentName);

    // Factures émises dans une période
    List<Invoice> findByIssuedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Factures échues
    @Query("SELECT i FROM Invoice i WHERE i.status = 'ISSUED' AND i.dueDate < :now")
    List<Invoice> findOverdueInvoices(@Param("now") LocalDateTime now);

    // Factures payées dans une période
    @Query("SELECT i FROM Invoice i WHERE i.status = 'PAID' AND i.paidAt BETWEEN :startDate AND :endDate")
    List<Invoice> findPaidInvoicesByPeriod(@Param("startDate") LocalDateTime startDate,
                                           @Param("endDate") LocalDateTime endDate);

    // Statistiques de facturation
    @Query("SELECT COUNT(i), SUM(i.amountTTC) FROM Invoice i WHERE i.status = 'PAID' AND i.issuedAt BETWEEN :startDate AND :endDate")
    Object[] findInvoiceStatsByPeriod(@Param("startDate") LocalDateTime startDate,
                                      @Param("endDate") LocalDateTime endDate);
}