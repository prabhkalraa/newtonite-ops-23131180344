package com.newtonite.ops.workitem;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public class AssignWorkItemRequest {

    // Nullable if unassigning, or non-null to assign to user
    private UUID assigneeId;

    @NotNull(message = "Version is required for optimistic locking")
    private Long version;

    public AssignWorkItemRequest() {}

    public AssignWorkItemRequest(UUID assigneeId, Long version) {
        this.assigneeId = assigneeId;
        this.version = version;
    }

    public UUID getAssigneeId() {
        return assigneeId;
    }

    public void setAssigneeId(UUID assigneeId) {
        this.assigneeId = assigneeId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }
}
