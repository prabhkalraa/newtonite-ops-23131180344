package com.newtonite.ops.dashboard;

import com.newtonite.ops.common.PageResponse;
import com.newtonite.ops.workitem.WorkItemResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    @GetMapping("/assigned-to-me")
    public ResponseEntity<PageResponse<WorkItemResponse>> getAssignedToMe(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getAssignedToMe(pageable));
    }

    @GetMapping("/unassigned-urgent")
    public ResponseEntity<PageResponse<WorkItemResponse>> getUnassignedUrgent(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getUnassignedUrgent(pageable));
    }

    @GetMapping("/awaiting-approval")
    public ResponseEntity<PageResponse<WorkItemResponse>> getAwaitingApproval(
            @PageableDefault(size = 10, sort = "updatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(dashboardService.getAwaitingApproval(pageable));
    }
}
