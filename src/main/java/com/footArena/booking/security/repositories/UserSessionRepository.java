package com.footArena.booking.security.repositories;

import com.footArena.booking.security.entities.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionRepository extends JpaRepository<UserSession, UUID> {

    Optional<UserSession> findBySessionToken(String sessionToken);

    List<UserSession> findByUserIdAndIsActiveTrueOrderByLastActivityDesc(UUID userId);

    List<UserSession> findByUserIdOrderByCreatedAtDesc(UUID userId);

    @Query("SELECT us FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true AND us.expiresAt > :now")
    List<UserSession> findActiveSessionsByUser(@Param("userId") UUID userId, @Param("now") LocalDateTime now);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.user.id = :userId")
    void deactivateAllSessionsByUser(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE UserSession us SET us.isActive = false WHERE us.expiresAt < :now")
    void cleanupExpiredSessions(@Param("now") LocalDateTime now);

    @Query("SELECT COUNT(us) FROM UserSession us WHERE us.user.id = :userId AND us.isActive = true")
    long countActiveSessionsByUser(@Param("userId") UUID userId);

    @Query("SELECT us FROM UserSession us WHERE us.ipAddress = :ipAddress AND us.createdAt > :since")
    List<UserSession> findRecentSessionsByIp(@Param("ipAddress") String ipAddress, @Param("since") LocalDateTime since);

    boolean existsBySessionTokenAndIsActiveTrue(String sessionToken);
}