package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository  extends JpaRepository<Notification, UUID> {

    List<Notification> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Notification>findByUserIdAndIsReadFalseOrderByCreatedAtDesc(UUID userId);
}
