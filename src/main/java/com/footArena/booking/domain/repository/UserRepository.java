package com.footArena.booking.domain.repository;

import com.footArena.booking.domain.model.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

}