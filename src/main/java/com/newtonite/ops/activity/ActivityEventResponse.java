package com.newtonite.ops.activity;

import java.time.Instant;
import java.util.UUID;

public class ActivityEventResponse {
    private UUID id;
    private UUID workItemId;
    private UUID actorId;
    private String actorName;
    private ActivityEventType eventType;
    private String oldValue;
    private String newValue;
    private String message;
    private Instant createdAt;

    public ActivityEventResponse() {}

    public ActivityEventResponse(UUID id, UUID workItemId, UUID actorId, String actorName,
                                 ActivityEventType eventType, String oldValue, String newValue,
                                 String message, Instant createdAt) {
        this.id = id;
        this.workItemId = workItemId;
        this.actorId = actorId;
        this.actorName = actorName;
        this.eventType = eventType;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.message = message;
        this.createdAt = createdAt;
    }

    public static ActivityEventResponse fromEntity(ActivityEvent event) {
        if (event == null) return null;
        return new ActivityEventResponse(
                event.getId(),
                event.getWorkItemId(),
                event.getActorId(),
                event.getActorName(),
                event.getEventType(),
                event.getOldValue(),
                event.getNewValue(),
                event.getMessage(),
                event.getCreatedAt()
        );
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getWorkItemId() {
        return workItemId;
    }

    public void setWorkItemId(UUID workItemId) {
        this.workItemId = workItemId;
    }

    public UUID getActorId() {
        return actorId;
    }

    public void setActorId(UUID actorId) {
        this.actorId = actorId;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public ActivityEventType getEventType() {
        return eventType;
    }

    public void setEventType(ActivityEventType eventType) {
        this.eventType = eventType;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
