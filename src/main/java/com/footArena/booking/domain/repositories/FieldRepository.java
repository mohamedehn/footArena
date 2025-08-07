package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Field;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FieldRepository extends JpaRepository<Field, UUID> {

    List<Field> findByEstablishmentId(UUID establishmentId);

    List<Field> findByAvailableTrue();

    List<Field> findByAvailableFalse();

    List<Field> findBySurfaceType(String surfaceType);

    List<Field> findByCapacityBetween(int minCapacity, int maxCapacity);

    @Query("SELECT f FROM Field f WHERE f.establishment.id = :establishmentId AND f.available = true")
    List<Field> findAvailableFieldsByEstablishment(@Param("establishmentId") UUID establishmentId);

    @Query("SELECT f FROM Field f WHERE f.name LIKE %:name% AND f.available = true")
    List<Field> findAvailableFieldsByNameContaining(@Param("name") String name);

    boolean existsByNameAndEstablishmentId(String name, UUID establishmentId);
}