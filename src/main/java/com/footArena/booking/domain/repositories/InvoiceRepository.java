package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {
    // Additional query methods can be defined here if needed
}