package com.newtonite.ops.team;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TeamMembershipRepository extends JpaRepository<TeamMembership, UUID> {

    @Query("SELECT tm FROM TeamMembership tm JOIN FETCH tm.team WHERE tm.user.id = :userId")
    List<TeamMembership> findByUserIdWithTeam(@Param("userId") UUID userId);

    @Query("SELECT tm FROM TeamMembership tm JOIN FETCH tm.user WHERE tm.team.id = :teamId")
    List<TeamMembership> findByTeamIdWithUser(@Param("teamId") UUID teamId);

    Optional<TeamMembership> findByUserIdAndTeamId(UUID userId, UUID teamId);

    boolean existsByUserIdAndTeamId(UUID userId, UUID teamId);

    @Query("SELECT tm.team.id FROM TeamMembership tm WHERE tm.user.id = :userId")
    List<UUID> findTeamIdsByUserId(@Param("userId") UUID userId);
}
