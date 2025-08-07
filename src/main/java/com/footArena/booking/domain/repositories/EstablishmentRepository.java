package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Establishment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EstablishmentRepository extends JpaRepository<Establishment, UUID> {
    boolean existsByEmail(String email);

    List<Establishment> findByNameContainingIgnoreCase(String name);

    List<Establishment> findByAddressContainingIgnoreCase(String address);

    @Query("SELECT e FROM Establishment e WHERE SIZE(e.fields) > 0")
    List<Establishment> findEstablishmentsWithFields();

    @Query("SELECT e FROM Establishment e JOIN FETCH e.fields WHERE e.id = :id")
    Establishment findByIdWithFields(@Param("id") UUID id);
}