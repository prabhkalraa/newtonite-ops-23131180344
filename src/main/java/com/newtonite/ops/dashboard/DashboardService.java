package com.newtonite.ops.dashboard;

import com.newtonite.ops.common.PageResponse;
import com.newtonite.ops.security.AuthorizationService;
import com.newtonite.ops.security.UserPrincipal;
import com.newtonite.ops.team.TeamRole;
import com.newtonite.ops.workitem.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final WorkItemRepository workItemRepository;
    private final AuthorizationService authorizationService;
    private final WorkflowService workflowService;

    public DashboardService(
            WorkItemRepository workItemRepository,
            AuthorizationService authorizationService,
            WorkflowService workflowService) {
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.workflowService = workflowService;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        UserPrincipal currentUser = authorizationService.getCurrentUser();
        Set<UUID> userTeams = currentUser.getTeamIds();

        if (userTeams.isEmpty()) {
            return new DashboardSummaryResponse(0, 0, 0, 0, Collections.emptyList());
        }

        long totalOpen = workItemRepository.countOpenItemsInTeams(userTeams, WorkItemStatus.CLOSED);
        long assignedToMe = workItemRepository.countAssignedToUserInTeams(userTeams, currentUser.getId(), WorkItemStatus.CLOSED);
        long unassignedUrgent = workItemRepository.countUnassignedUrgentInTeams(userTeams, WorkItemPriority.URGENT, WorkItemStatus.CLOSED);

        // Teams where user has Lead or Admin approval role
        Set<UUID> approverTeams = userTeams.stream()
                .filter(teamId -> currentUser.hasTeamRoleAtLeast(teamId, TeamRole.LEAD))
                .collect(Collectors.toSet());

        long awaitingApproval = approverTeams.isEmpty() ? 0 :
                workItemRepository.countByTeamsAndStatus(approverTeams, WorkItemStatus.AWAITING_APPROVAL);

        List<WorkItem> recent = workItemRepository.findRecentItemsInTeams(userTeams, PageRequest.of(0, 5));
        List<WorkItemResponse> recentResponses = recent.stream()
                .map(item -> enrich(item, currentUser))
                .toList();

        return new DashboardSummaryResponse(totalOpen, assignedToMe, unassignedUrgent, awaitingApproval, recentResponses);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> getAssignedToMe(Pageable pageable) {
        UserPrincipal currentUser = authorizationService.getCurrentUser();
        Set<UUID> userTeams = currentUser.getTeamIds();

        Page<WorkItem> page = workItemRepository.findAll(
                WorkItemSpecification.filter(
                        userTeams, null, null, null, null, currentUser.getId(), null, null, null, null
                ),
                pageable
        );

        return toPageResponse(page, currentUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> getUnassignedUrgent(Pageable pageable) {
        UserPrincipal currentUser = authorizationService.getCurrentUser();
        Set<UUID> userTeams = currentUser.getTeamIds();

        Page<WorkItem> page = workItemRepository.findAll(
                WorkItemSpecification.filter(
                        userTeams, null, null, WorkItemPriority.URGENT, null, null, true, null, null, null
                ),
                pageable
        );

        return toPageResponse(page, currentUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<WorkItemResponse> getAwaitingApproval(Pageable pageable) {
        UserPrincipal currentUser = authorizationService.getCurrentUser();
        Set<UUID> userTeams = currentUser.getTeamIds();

        // Must be restricted to teams where current user has approval permission
        Set<UUID> approverTeams = userTeams.stream()
                .filter(teamId -> currentUser.hasTeamRoleAtLeast(teamId, TeamRole.LEAD))
                .collect(Collectors.toSet());

        if (approverTeams.isEmpty()) {
            return new PageResponse<>(Collections.emptyList(), 0, pageable.getPageSize(), 0, 0);
        }

        Page<WorkItem> page = workItemRepository.findAll(
                WorkItemSpecification.filter(
                        approverTeams, null, WorkItemStatus.AWAITING_APPROVAL, null, null, null, null, null, null, null
                ),
                pageable
        );

        return toPageResponse(page, currentUser);
    }

    private PageResponse<WorkItemResponse> toPageResponse(Page<WorkItem> page, UserPrincipal currentUser) {
        List<WorkItemResponse> dtos = page.getContent().stream()
                .map(item -> enrich(item, currentUser))
                .toList();
        return new PageResponse<>(
                dtos, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages()
        );
    }

    private WorkItemResponse enrich(WorkItem item, UserPrincipal currentUser) {
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
