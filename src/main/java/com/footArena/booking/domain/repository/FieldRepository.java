package com.footArena.booking.domain.repository;

import com.footArena.booking.domain.model.entity.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface FieldRepository extends JpaRepository<Field, UUID> {

}