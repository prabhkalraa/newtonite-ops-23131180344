package com.newtonite.ops.activity;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ActivityEventService {

    private final ActivityEventRepository activityEventRepository;

    public ActivityEventService(ActivityEventRepository activityEventRepository) {
        this.activityEventRepository = activityEventRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ActivityEvent recordEvent(UUID workItemId, UUID actorId, String actorName,
                                      ActivityEventType eventType, String oldValue, String newValue, String message) {
        ActivityEvent event = new ActivityEvent(
                UUID.randomUUID(),
                workItemId,
                actorId,
                actorName,
                eventType,
                oldValue,
                newValue,
                message
        );
        return activityEventRepository.save(event);
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getEventsForWorkItem(UUID workItemId, Pageable pageable) {
        return activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(workItemId, pageable)
                .map(ActivityEventResponse::fromEntity);
    }
}
