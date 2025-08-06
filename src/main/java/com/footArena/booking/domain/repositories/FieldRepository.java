package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FieldRepository extends JpaRepository<Field, UUID> {

}