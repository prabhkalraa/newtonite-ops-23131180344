package com.newtonite.ops.workitem;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.activity.ActivityEventResponse;
import com.newtonite.ops.activity.ActivityEventService;
import com.newtonite.ops.activity.ActivityEventType;
import com.newtonite.ops.common.PageResponse;
import com.newtonite.ops.exception.ConflictException;
import com.newtonite.ops.exception.ForbiddenException;
import com.newtonite.ops.exception.ResourceNotFoundException;
import com.newtonite.ops.idempotency.IdempotencyRecord;
import com.newtonite.ops.idempotency.IdempotencyService;
import com.newtonite.ops.outbox.OutboxService;
import com.newtonite.ops.security.AuthorizationService;
import com.newtonite.ops.security.UserPrincipal;
import com.newtonite.ops.team.Team;
import com.newtonite.ops.team.TeamRepository;
import com.newtonite.ops.team.TeamRole;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.*;

@Service
public class WorkItemService {

    private final WorkItemRepository workItemRepository;
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final WorkflowService workflowService;
    private final ActivityEventService activityEventService;
    private final AuthorizationService authorizationService;
    private final IdempotencyService idempotencyService;
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;

    public WorkItemService(
            WorkItemRepository workItemRepository,
            TeamRepository teamRepository,
            UserRepository userRepository,
            WorkflowService workflowService,
            ActivityEventService activityEventService,
            AuthorizationService authorizationService,
            IdempotencyService idempotencyService,
            OutboxService outboxService,
            ObjectMapper objectMapper) {
        this.workItemRepository = workItemRepository;
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
        this.activityEventService = activityEventService;
        this.authorizationService = authorizationService;
        this.idempotencyService = idempotencyService;
        this.outboxService = outboxService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public WorkItemResponse createWorkItem(CreateWorkItemRequest request, String idempotencyKey) {
        UserPrincipal currentUser = authorizationService.getCurrentUser();
        authorizationService.requireRoleAtLeast(request.getTeamId(), TeamRole.MEMBER);

        String requestHash = null;
        if (StringUtils.hasText(idempotencyKey)) {
            requestHash = idempotencyService.computeHash(request);
            Optional<IdempotencyRecord> existing = idempotencyService.findExisting(
                    idempotencyKey, currentUser.getId(), "CREATE_WORK_ITEM", requestHash
            );
            if (existing.isPresent()) {
                try {
                    return objectMapper.readValue(existing.get().getResponseBody(), WorkItemResponse.class);
                } catch (Exception e) {
                    throw new RuntimeException("Failed to deserialize idempotency response", e);
                }
            }
        }

        Team team = teamRepository.findById(request.getTeamId())
                .orElseThrow(() -> new ResourceNotFoundException("Team not found: " + request.getTeamId()));

        User creator = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        User assignee = null;
        if (request.getAssigneeId() != null) {
            authorizationService.verifyAssigneeInTeam(request.getAssigneeId(), team.getId());
            assignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found: " + request.getAssigneeId()));
        }

        WorkItem item = new WorkItem(
                UUID.randomUUID(),
                request.getTitle(),
                request.getDescription(),
                request.getType(),
                request.getPriority(),
                WorkItemStatus.OPEN,
                team,
                assignee,
                creator,
                request.getDueDate()
        );

        WorkItem saved = workItemRepository.saveAndFlush(item);

        // Transactional Activity History
        activityEventService.recordEvent(
                saved.getId(),
                currentUser.getId(),
                currentUser.getDisplayName(),
                ActivityEventType.CREATED,
                null,
                saved.getStatus().name(),
                "Created work item: " + saved.getTitle()
        );

        // Transactional Outbox Job
        Map<String, Object> outboxPayload = new HashMap<>();
        outboxPayload.put("itemId", saved.getId().toString());
        outboxPayload.put("title", saved.getTitle());
        outboxPayload.put("team", team.getName());
        outboxPayload.put("createdBy", creator.getDisplayName());
        outboxService.publishEvent("WORK_ITEM_CREATED", outboxPayload);

        WorkItemResponse response = enrichResponse(saved, currentUser);

        if (StringUtils.hasText(idempotencyKey) && requestHash != null) {
            try {
                String responseJson = objectMapper.writeValueAsString(response);
                idempotencyService.saveRecord(
                        idempotencyKey, currentUser.getId(), "CREATE_WORK_ITEM", requestHash, 201, responseJson
                );
            } catch (Exception e) {
                throw new RuntimeException("Failed to persist idempotency record", e);
            }
        }

        return response;
    }

    @Transactional(readOnly = true)
    public WorkItemResponse getWorkItem(UUID id) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        authorizationService.requireTeamAccess(item.getTeam().getId());
        return enrichResponse(item, authorizationService.getCurrentUser());
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> getWorkItems(
            UUID teamId,
            WorkItemStatus status,
            WorkItemPriority priority,
            WorkItemType type,
            UUID assigneeId,
            Boolean unassignedOnly,
            String search,
            Instant dueBefore,
            Instant dueAfter,
            Pageable pageable) {

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        Set<UUID> authorizedTeams = currentUser.getTeamIds();

        Page<WorkItem> page = workItemRepository.findAll(
                WorkItemSpecification.filter(
                        authorizedTeams, teamId, status, priority, type,
                        assigneeId, unassignedOnly, search, dueBefore, dueAfter
                ),
                pageable
        );

        List<WorkItemResponse> dtos = page.getContent().stream()
                .map(item -> enrichResponse(item, currentUser))
                .toList();

        return new PageResponse<>(
                dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    @Transactional
    public WorkItemResponse updateWorkItem(UUID id, UpdateWorkItemRequest request) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        authorizationService.requireRoleAtLeast(item.getTeam().getId(), TeamRole.MEMBER);

        if (item.getStatus() == WorkItemStatus.CLOSED) {
            throw new ConflictException("Cannot modify a CLOSED work item");
        }

        // Optimistic locking verification
        if (!Objects.equals(item.getVersion(), request.getVersion())) {
            throw new ConflictException("This item has changed since you opened it. Reload the latest version before editing.");
        }

        boolean updated = false;

        if (StringUtils.hasText(request.getTitle()) && !request.getTitle().equals(item.getTitle())) {
            item.setTitle(request.getTitle());
            updated = true;
        }

        if (request.getDescription() != null && !request.getDescription().equals(item.getDescription())) {
            item.setDescription(request.getDescription());
            updated = true;
        }

        if (request.getPriority() != null && request.getPriority() != item.getPriority()) {
            WorkItemPriority oldPriority = item.getPriority();
            item.setPriority(request.getPriority());
            activityEventService.recordEvent(
                    item.getId(),
                    currentUser.getId(),
                    currentUser.getDisplayName(),
                    ActivityEventType.PRIORITY_CHANGED,
                    oldPriority.name(),
                    item.getPriority().name(),
                    "Priority changed from " + oldPriority + " to " + item.getPriority()
            );
            updated = true;
        }

        if (request.getDueDate() != null && !request.getDueDate().equals(item.getDueDate())) {
            item.setDueDate(request.getDueDate());
            updated = true;
        }

        if (updated) {
            activityEventService.recordEvent(
                    item.getId(),
                    currentUser.getId(),
                    currentUser.getDisplayName(),
                    ActivityEventType.DETAILS_UPDATED,
                    null,
                    null,
                    "Updated item details"
            );
            item = workItemRepository.saveAndFlush(item);
        }

        return enrichResponse(item, currentUser);
    }

    @Transactional
    public WorkItemResponse claimWorkItem(UUID id) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        authorizationService.requireRoleAtLeast(item.getTeam().getId(), TeamRole.MEMBER);

        if (item.getStatus() == WorkItemStatus.CLOSED) {
            throw new ConflictException("Cannot claim a CLOSED work item");
        }

        if (item.getAssignee() != null) {
            throw new ConflictException("Work item has already been claimed by " + item.getAssignee().getDisplayName());
        }

        User userEntity = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + currentUser.getId()));

        // Concurrency-safe atomic conditional claim
        int updatedRows = workItemRepository.claimAtomically(id, userEntity, Instant.now());
        if (updatedRows == 0) {
            throw new ConflictException("Work item has already been claimed or is no longer available.");
        }

        // Reload updated item
        WorkItem updatedItem = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        // Audit Activity Event
        activityEventService.recordEvent(
                id,
                currentUser.getId(),
                currentUser.getDisplayName(),
                ActivityEventType.CLAIMED,
                "Unassigned",
                currentUser.getDisplayName(),
                "Claimed work item"
        );

        // Outbox notification event
        Map<String, Object> outboxPayload = new HashMap<>();
        outboxPayload.put("itemId", updatedItem.getId().toString());
        outboxPayload.put("title", updatedItem.getTitle());
        outboxPayload.put("assignee", currentUser.getDisplayName());
        outboxService.publishEvent("WORK_ITEM_ASSIGNED", outboxPayload);

        return enrichResponse(updatedItem, currentUser);
    }

    @Transactional
    public WorkItemResponse assignWorkItem(UUID id, AssignWorkItemRequest request) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        authorizationService.requireRoleAtLeast(item.getTeam().getId(), TeamRole.LEAD);

        if (item.getStatus() == WorkItemStatus.CLOSED) {
            throw new ConflictException("Cannot assign a CLOSED work item");
        }

        // Optimistic locking verification
        if (!Objects.equals(item.getVersion(), request.getVersion())) {
            throw new ConflictException("This item has changed since you opened it. Reload the latest version before editing.");
        }

        String oldAssigneeName = item.getAssignee() != null ? item.getAssignee().getDisplayName() : "Unassigned";

        if (request.getAssigneeId() != null) {
            authorizationService.verifyAssigneeInTeam(request.getAssigneeId(), item.getTeam().getId());
            User newAssignee = userRepository.findById(request.getAssigneeId())
                    .orElseThrow(() -> new ResourceNotFoundException("Assignee not found: " + request.getAssigneeId()));

            item.setAssignee(newAssignee);
            activityEventService.recordEvent(
                    item.getId(),
                    currentUser.getId(),
                    currentUser.getDisplayName(),
                    ActivityEventType.ASSIGNED,
                    oldAssigneeName,
                    newAssignee.getDisplayName(),
                    "Assigned to " + newAssignee.getDisplayName()
            );

            Map<String, Object> outboxPayload = new HashMap<>();
            outboxPayload.put("itemId", item.getId().toString());
            outboxPayload.put("title", item.getTitle());
            outboxPayload.put("assignee", newAssignee.getDisplayName());
            outboxService.publishEvent("WORK_ITEM_ASSIGNED", outboxPayload);
        } else {
            item.setAssignee(null);
            activityEventService.recordEvent(
                    item.getId(),
                    currentUser.getId(),
                    currentUser.getDisplayName(),
                    ActivityEventType.UNASSIGNED,
                    oldAssigneeName,
                    "Unassigned",
                    "Removed assignee"
            );
        }

        WorkItem saved = workItemRepository.saveAndFlush(item);
        return enrichResponse(saved, currentUser);
    }

    @Transactional
    public WorkItemResponse transitionWorkItem(UUID id, TransitionWorkItemRequest request) {
        WorkItem item = workItemRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        TeamRole userRole = currentUser.getRoleForTeam(item.getTeam().getId())
                .orElseThrow(() -> new ForbiddenException("You are not a member of team " + item.getTeam().getId()));

        // Optimistic locking verification
        if (!Objects.equals(item.getVersion(), request.getVersion())) {
            throw new ConflictException("This item has changed since you opened it. Reload the latest version before editing.");
        }

        WorkItemStatus oldStatus = item.getStatus();
        workflowService.validateTransition(oldStatus, request.getTargetStatus(), userRole);

        item.setStatus(request.getTargetStatus());
        WorkItem saved = workItemRepository.saveAndFlush(item);

        activityEventService.recordEvent(
                item.getId(),
                currentUser.getId(),
                currentUser.getDisplayName(),
                ActivityEventType.STATUS_CHANGED,
                oldStatus.name(),
                saved.getStatus().name(),
                "Status transitioned from " + oldStatus + " to " + saved.getStatus()
        );

        Map<String, Object> outboxPayload = new HashMap<>();
        outboxPayload.put("itemId", saved.getId().toString());
        outboxPayload.put("title", saved.getTitle());
        outboxPayload.put("oldStatus", oldStatus.name());
        outboxPayload.put("newStatus", saved.getStatus().name());
        outboxService.publishEvent("WORK_ITEM_STATUS_CHANGED", outboxPayload);

        return enrichResponse(saved, currentUser);
    }

    @Transactional(readOnly = true)
    public Page<ActivityEventResponse> getActivityHistory(UUID id, Pageable pageable) {
        WorkItem item = workItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + id));

        authorizationService.requireTeamAccess(item.getTeam().getId());
        return activityEventService.getEventsForWorkItem(id, pageable);
    }

    private WorkItemResponse enrichResponse(WorkItem item, UserPrincipal currentUser) {
        WorkItemResponse resp = WorkItemResponse.fromEntity(item);
        UUID teamId = item.getTeam().getId();
        TeamRole role = currentUser.getRoleForTeam(teamId).orElse(null);

        if (role != null) {
            resp.setCanComment(role.isAtLeast(TeamRole.MEMBER));
            resp.setCanEdit(role.isAtLeast(TeamRole.MEMBER) && item.getStatus() != WorkItemStatus.CLOSED);
            resp.setCanClaim(role.isAtLeast(TeamRole.MEMBER) && item.getAssignee() == null && item.getStatus() != WorkItemStatus.CLOSED);
            resp.setCanAssign(role.isAtLeast(TeamRole.LEAD) && item.getStatus() != WorkItemStatus.CLOSED);
            resp.setAvailableTransitions(workflowService.getAvailableTransitions(item.getStatus(), role));
        } else {
            resp.setAvailableTransitions(Collections.emptySet());
        }
        return resp;
    }
}
