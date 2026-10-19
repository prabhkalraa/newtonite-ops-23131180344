package com.newtonite.ops.workitem;

import jakarta.validation.constraints.NotNull;

public class TransitionWorkItemRequest {

    @NotNull(message = "Target status is required")
    private WorkItemStatus targetStatus;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    public TransitionWorkItemRequest() {}

    public TransitionWorkItemRequest(WorkItemStatus targetStatus, Long version) {
        this.targetStatus = targetStatus;
        this.version = version;
    }

    public WorkItemStatus getTargetStatus() {
        return targetStatus;
    }

    public void setTargetStatus(WorkItemStatus targetStatus) {
        this.targetStatus = targetStatus;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
