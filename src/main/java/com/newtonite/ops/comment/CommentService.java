package com.newtonite.ops.comment;

import com.newtonite.ops.activity.ActivityEventService;
import com.newtonite.ops.activity.ActivityEventType;
import com.newtonite.ops.exception.ResourceNotFoundException;
import com.newtonite.ops.security.AuthorizationService;
import com.newtonite.ops.security.UserPrincipal;
import com.newtonite.ops.team.TeamRole;
import com.newtonite.ops.workitem.WorkItem;
import com.newtonite.ops.workitem.WorkItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final WorkItemRepository workItemRepository;
    private final AuthorizationService authorizationService;
    private final ActivityEventService activityEventService;

    public CommentService(
            CommentRepository commentRepository,
            WorkItemRepository workItemRepository,
            AuthorizationService authorizationService,
            ActivityEventService activityEventService) {
        this.commentRepository = commentRepository;
        this.workItemRepository = workItemRepository;
        this.authorizationService = authorizationService;
        this.activityEventService = activityEventService;
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getComments(UUID workItemId) {
        WorkItem item = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + workItemId));

        authorizationService.requireTeamAccess(item.getTeam().getId());

        return commentRepository.findByWorkItemIdOrderByCreatedAtAsc(workItemId).stream()
                .map(CommentResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(UUID workItemId, CommentRequest request) {
        WorkItem item = workItemRepository.findById(workItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Work item not found: " + workItemId));

        UserPrincipal currentUser = authorizationService.getCurrentUser();
        authorizationService.requireRoleAtLeast(item.getTeam().getId(), TeamRole.MEMBER);

        Comment comment = new Comment(
                UUID.randomUUID(),
                workItemId,
                currentUser.getId(),
                currentUser.getDisplayName(),
                request.getContent()
        );

        Comment saved = commentRepository.save(comment);

        // Record activity event in the same transaction
        activityEventService.recordEvent(
                workItemId,
                currentUser.getId(),
                currentUser.getDisplayName(),
                ActivityEventType.COMMENT_ADDED,
                null,
                null,
                "Added comment: " + (request.getContent().length() > 50 ?
                        request.getContent().substring(0, 50) + "..." : request.getContent())
        );

        return CommentResponse.fromEntity(saved);
    }
}
