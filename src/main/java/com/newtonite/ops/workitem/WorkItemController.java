package com.newtonite.ops.workitem;

import com.newtonite.ops.activity.ActivityEventResponse;
import com.newtonite.ops.common.PageResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/items")
public class WorkItemController {

    private final WorkItemService workItemService;

    public WorkItemController(WorkItemService workItemService) {
        this.workItemService = workItemService;
    }

    @PostMapping
    public ResponseEntity<WorkItemResponse> createWorkItem(
            @Valid @RequestBody CreateWorkItemRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        WorkItemResponse response = workItemService.createWorkItem(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<PageResponse<WorkItemResponse>> getWorkItems(
            @RequestParam(required = false) UUID teamId,
            @RequestParam(required = false) WorkItemStatus status,
            @RequestParam(required = false) WorkItemPriority priority,
            @RequestParam(required = false) WorkItemType type,
            @RequestParam(required = false) UUID assigneeId,
            @RequestParam(required = false) Boolean unassignedOnly,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Instant dueBefore,
            @RequestParam(required = false) Instant dueAfter,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        return ResponseEntity.ok(workItemService.getWorkItems(
                teamId, status, priority, type, assigneeId, unassignedOnly, search, dueBefore, dueAfter, pageable
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<WorkItemResponse> getWorkItem(@PathVariable UUID id) {
        return ResponseEntity.ok(workItemService.getWorkItem(id));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<WorkItemResponse> updateWorkItem(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.updateWorkItem(id, request));
    }

    @PostMapping("/{id}/claim")
    public ResponseEntity<WorkItemResponse> claimWorkItem(@PathVariable UUID id) {
        return ResponseEntity.ok(workItemService.claimWorkItem(id));
    }

    @PostMapping("/{id}/assign")
    public ResponseEntity<WorkItemResponse> assignWorkItem(
            @PathVariable UUID id,
            @Valid @RequestBody AssignWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.assignWorkItem(id, request));
    }

    @PostMapping("/{id}/transition")
    public ResponseEntity<WorkItemResponse> transitionWorkItem(
            @PathVariable UUID id,
            @Valid @RequestBody TransitionWorkItemRequest request) {
        return ResponseEntity.ok(workItemService.transitionWorkItem(id, request));
    }

    @GetMapping("/{id}/activity")
    public ResponseEntity<PageResponse<ActivityEventResponse>> getActivityHistory(
            @PathVariable UUID id,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ActivityEventResponse> page = workItemService.getActivityHistory(id, pageable);
        return ResponseEntity.ok(PageResponse.of(page));
    }
}
