package com.newtonite.ops.workitem;

import com.newtonite.ops.exception.AppException;
import com.newtonite.ops.exception.ConflictException;
import com.newtonite.ops.exception.ForbiddenException;
import com.newtonite.ops.team.TeamRole;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WorkflowService {

    // Valid transitions map: sourceStatus -> set of allowed target statuses
    private static final Map<WorkItemStatus, Set<WorkItemStatus>> ALLOWED_TRANSITIONS = new EnumMap<>(WorkItemStatus.class);

    static {
        ALLOWED_TRANSITIONS.put(WorkItemStatus.OPEN, Set.of(WorkItemStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(WorkItemStatus.IN_PROGRESS, Set.of(WorkItemStatus.AWAITING_APPROVAL, WorkItemStatus.RESOLVED));
        ALLOWED_TRANSITIONS.put(WorkItemStatus.AWAITING_APPROVAL, Set.of(WorkItemStatus.RESOLVED, WorkItemStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(WorkItemStatus.RESOLVED, Set.of(WorkItemStatus.CLOSED, WorkItemStatus.IN_PROGRESS));
        ALLOWED_TRANSITIONS.put(WorkItemStatus.CLOSED, Collections.emptySet());
    }

    public void validateTransition(WorkItemStatus currentStatus, WorkItemStatus targetStatus, TeamRole userRole) {
        if (currentStatus == targetStatus) {
            throw new ConflictException("Item is already in status " + targetStatus);
        }

        if (currentStatus == WorkItemStatus.CLOSED) {
            throw new ConflictException("Cannot transition a CLOSED work item");
        }

        Set<WorkItemStatus> allowed = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        if (!allowed.contains(targetStatus)) {
            throw new ConflictException(
                    String.format("Invalid workflow transition from %s to %s. Allowed targets: %s",
                            currentStatus, targetStatus, allowed)
            );
        }

        // Approval check: Transitioning from AWAITING_APPROVAL to RESOLVED is an approval action.
        if (currentStatus == WorkItemStatus.AWAITING_APPROVAL && targetStatus == WorkItemStatus.RESOLVED) {
            if (!userRole.isAtLeast(TeamRole.LEAD)) {
                throw new ForbiddenException("Only a team Lead or Admin can approve an item awaiting approval");
            }
        }

        // Reopening check: Transitioning from RESOLVED back to IN_PROGRESS requires at least MEMBER
        if (currentStatus == WorkItemStatus.RESOLVED && targetStatus == WorkItemStatus.IN_PROGRESS) {
            if (!userRole.isAtLeast(TeamRole.MEMBER)) {
                throw new ForbiddenException("Only members or leads can reopen a resolved item");
            }
        }
    }

    public Set<WorkItemStatus> getAvailableTransitions(WorkItemStatus currentStatus, TeamRole userRole) {
        if (currentStatus == null || currentStatus == WorkItemStatus.CLOSED || userRole == null) {
            return Collections.emptySet();
        }

        Set<WorkItemStatus> possible = ALLOWED_TRANSITIONS.getOrDefault(currentStatus, Collections.emptySet());
        Set<WorkItemStatus> permitted = new HashSet<>();

        for (WorkItemStatus target : possible) {
            try {
                validateTransition(currentStatus, target, userRole);
                permitted.add(target);
            } catch (AppException ignored) {
                // Not permitted for this role
            }
        }
        return permitted;
    }
}
