package com.newtonite.ops.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findByWorkItemIdOrderByCreatedAtDesc(UUID workItemId);
    Page<ActivityEvent> findByWorkItemIdOrderByCreatedAtDesc(UUID workItemId, Pageable pageable);
}
