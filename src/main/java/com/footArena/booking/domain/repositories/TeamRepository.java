package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.Team;
import com.footArena.booking.domain.enums.SkillLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TeamRepository extends JpaRepository<Team, UUID> {

    List<Team> findByIsPublicTrue();

    List<Team> findByCaptainId(UUID captainId);

    List<Team> findBySkillLevel(SkillLevel skillLevel);

    @Query("SELECT t FROM Team t WHERE t.currentPlayers < t.maxPlayers AND t.isPublic = true")
    List<Team> findAvailablePublicTeams();

    @Query("SELECT t FROM Team t WHERE t.name LIKE %:name%")
    List<Team> findByNameContaining(@Param("name") String name);

    @Query("SELECT t FROM Team t JOIN t.members tm WHERE tm.user.id = :userId AND tm.status = 'ACTIVE'")
    List<Team> findTeamsByMemberId(@Param("userId") UUID userId);
}