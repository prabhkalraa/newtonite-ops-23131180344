package com.newtonite.ops.outbox;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxJobRepository extends JpaRepository<OutboxJob, UUID> {

    @Query("SELECT j FROM OutboxJob j WHERE j.status = 'PENDING' AND j.nextAttemptAt <= :now ORDER BY j.createdAt ASC")
    List<OutboxJob> findDuePendingJobs(@Param("now") Instant now, Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE OutboxJob j SET j.status = 'PROCESSING', j.updatedAt = :now WHERE j.id = :id AND j.status = 'PENDING'")
    int claimJobAtomically(@Param("id") UUID id, @Param("now") Instant now);
}
