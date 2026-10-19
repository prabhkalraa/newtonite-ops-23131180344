package com.newtonite.ops.workitem;

import com.newtonite.ops.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkItemRepository extends JpaRepository<WorkItem, UUID>, JpaSpecificationExecutor<WorkItem> {

    @Query("SELECT w FROM WorkItem w " +
           "JOIN FETCH w.team " +
           "JOIN FETCH w.createdBy " +
           "LEFT JOIN FETCH w.assignee " +
           "WHERE w.id = :id")
    Optional<WorkItem> findByIdWithDetails(@Param("id") UUID id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE WorkItem w SET w.assignee = :assignee, w.version = w.version + 1, w.updatedAt = :now " +
           "WHERE w.id = :id AND w.assignee IS NULL")
    int claimAtomically(@Param("id") UUID id, @Param("assignee") User assignee, @Param("now") Instant now);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.team.id IN :teamIds AND w.status != :closedStatus")
    long countOpenItemsInTeams(@Param("teamIds") Collection<UUID> teamIds, @Param("closedStatus") WorkItemStatus closedStatus);

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.team.id IN :teamIds AND w.assignee.id = :assigneeId AND w.status != :closedStatus")
    long countAssignedToUserInTeams(
            @Param("teamIds") Collection<UUID> teamIds,
            @Param("assigneeId") UUID assigneeId,
            @Param("closedStatus") WorkItemStatus closedStatus
    );

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.team.id IN :teamIds AND w.assignee IS NULL AND w.priority = :priority AND w.status != :closedStatus")
    long countUnassignedUrgentInTeams(
            @Param("teamIds") Collection<UUID> teamIds,
            @Param("priority") WorkItemPriority priority,
            @Param("closedStatus") WorkItemStatus closedStatus
    );

    @Query("SELECT COUNT(w) FROM WorkItem w WHERE w.team.id IN :teamIds AND w.status = :status")
    long countByTeamsAndStatus(@Param("teamIds") Collection<UUID> teamIds, @Param("status") WorkItemStatus status);

    @Query("SELECT w FROM WorkItem w JOIN FETCH w.team LEFT JOIN FETCH w.assignee JOIN FETCH w.createdBy " +
           "WHERE w.team.id IN :teamIds ORDER BY w.updatedAt DESC")
    List<WorkItem> findRecentItemsInTeams(@Param("teamIds") Collection<UUID> teamIds, Pageable pageable);
}
