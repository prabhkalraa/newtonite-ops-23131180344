package com.newtonite.ops.workitem;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class WorkItemSpecification {

    public static Specification<WorkItem> filter(
            Collection<UUID> authorizedTeamIds,
            UUID teamId,
            WorkItemStatus status,
            WorkItemPriority priority,
            WorkItemType type,
            UUID assigneeId,
            Boolean unassignedOnly,
            String search,
            Instant dueBefore,
            Instant dueAfter
    ) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Mandatory Scope: Must be within teams the user has access to
            if (authorizedTeamIds == null || authorizedTeamIds.isEmpty()) {
                return cb.disjunction(); // No access to any teams
            }

            if (teamId != null) {
                if (!authorizedTeamIds.contains(teamId)) {
                    return cb.disjunction(); // Trying to filter for unauthorized team
                }
                predicates.add(cb.equal(root.get("team").get("id"), teamId));
            } else {
                predicates.add(root.get("team").get("id").in(authorizedTeamIds));
            }

            // 2. Status filter
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            // 3. Priority filter
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }

            // 4. Type filter
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type));
            }

            // 5. Assignee filter
            if (Boolean.TRUE.equals(unassignedOnly)) {
                predicates.add(cb.isNull(root.get("assignee")));
            } else if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assignee").get("id"), assigneeId));
            }

            // 6. Free text search across title and description
            if (StringUtils.hasText(search)) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                Predicate descMatch = cb.like(cb.lower(root.get("description")), pattern);
                predicates.add(cb.or(titleMatch, descMatch));
            }

            // 7. Due date range
            if (dueBefore != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("dueDate"), dueBefore));
            }
            if (dueAfter != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("dueDate"), dueAfter));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
