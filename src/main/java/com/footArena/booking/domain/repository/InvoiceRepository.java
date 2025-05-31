package com.footArena.booking.domain.repository;

import com.footArena.booking.domain.model.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    // Additional query methods can be defined here if needed
}