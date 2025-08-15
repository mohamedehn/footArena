package com.footArena.booking.domain.repositories;

import com.footArena.booking.domain.entities.TeamMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, UUID> {

    List<TeamMember> findByTeamId(UUID teamId);

    List<TeamMember> findByUserId(UUID userId);

    Optional<TeamMember> findByTeamIdAndUserId(UUID teamId, UUID userId);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.status = 'ACTIVE'")
    List<TeamMember> findActiveTeamMembers(@Param("teamId") UUID teamId);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.user.id = :userId AND tm.status = 'ACTIVE'")
    List<TeamMember> findActiveTeamsByUserId(@Param("userId") UUID userId);

    @Query("SELECT tm FROM TeamMember tm WHERE tm.team.id = :teamId AND tm.role = 'CAPTAIN'")
    Optional<TeamMember> findTeamCaptain(@Param("teamId") UUID teamId);
}