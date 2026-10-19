package com.newtonite.ops.workitem;

import com.newtonite.ops.team.TeamResponse;
import com.newtonite.ops.user.UserResponse;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public class WorkItemResponse {
    private UUID id;
    private String title;
    private String description;
    private WorkItemType type;
    private WorkItemPriority priority;
    private WorkItemStatus status;
    private TeamResponse team;
    private UserResponse assignee;
    private UserResponse createdBy;
    private Instant dueDate;
    private Long version;
    private Instant createdAt;
    private Instant updatedAt;
    private Set<WorkItemStatus> availableTransitions;
    private boolean canClaim;
    private boolean canEdit;
    private boolean canAssign;
    private boolean canComment;

    public WorkItemResponse() {}

    public static WorkItemResponse fromEntity(WorkItem item) {
        if (item == null) return null;
        WorkItemResponse resp = new WorkItemResponse();
        resp.setId(item.getId());
        resp.setTitle(item.getTitle());
        resp.setDescription(item.getDescription());
        resp.setType(item.getType());
        resp.setPriority(item.getPriority());
        resp.setStatus(item.getStatus());
        resp.setTeam(item.getTeam() != null ? TeamResponse.fromEntity(item.getTeam(), null) : null);
        resp.setAssignee(UserResponse.fromEntity(item.getAssignee()));
        resp.setCreatedBy(UserResponse.fromEntity(item.getCreatedBy()));
        resp.setDueDate(item.getDueDate());
        resp.setVersion(item.getVersion());
        resp.setCreatedAt(item.getCreatedAt());
        resp.setUpdatedAt(item.getUpdatedAt());
        return resp;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public WorkItemType getType() {
        return type;
    }

    public void setType(WorkItemType type) {
        this.type = type;
    }

    public WorkItemPriority getPriority() {
        return priority;
    }

    public void setPriority(WorkItemPriority priority) {
        this.priority = priority;
    }

    public WorkItemStatus getStatus() {
        return status;
    }

    public void setStatus(WorkItemStatus status) {
        this.status = status;
    }

    public TeamResponse getTeam() {
        return team;
    }

    public void setTeam(TeamResponse team) {
        this.team = team;
    }

    public UserResponse getAssignee() {
        return assignee;
    }

    public void setAssignee(UserResponse assignee) {
        this.assignee = assignee;
    }

    public UserResponse getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UserResponse createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getDueDate() {
        return dueDate;
    }

    public void setDueDate(Instant dueDate) {
        this.dueDate = dueDate;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Set<WorkItemStatus> getAvailableTransitions() {
        return availableTransitions;
    }

    public void setAvailableTransitions(Set<WorkItemStatus> availableTransitions) {
        this.availableTransitions = availableTransitions;
    }

    public boolean isCanClaim() {
        return canClaim;
    }

    public void setCanClaim(boolean canClaim) {
        this.canClaim = canClaim;
    }

    public boolean isCanEdit() {
        return canEdit;
    }

    public void setCanEdit(boolean canEdit) {
        this.canEdit = canEdit;
    }

    public boolean isCanAssign() {
        return canAssign;
    }

    public void setCanAssign(boolean canAssign) {
        this.canAssign = canAssign;
    }

    public boolean isCanComment() {
        return canComment;
    }

    public void setCanComment(boolean canComment) {
        this.canComment = canComment;
    }
}
