package com.newtonite.ops.security;

import com.newtonite.ops.exception.ForbiddenException;
import com.newtonite.ops.exception.UnauthorizedException;
import com.newtonite.ops.team.TeamMembershipRepository;
import com.newtonite.ops.team.TeamRole;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class AuthorizationService {

    private final TeamMembershipRepository teamMembershipRepository;

    public AuthorizationService(TeamMembershipRepository teamMembershipRepository) {
        this.teamMembershipRepository = teamMembershipRepository;
    }

    public UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof UserPrincipal)) {
            throw new UnauthorizedException("User is not authenticated");
        }
        return (UserPrincipal) auth.getPrincipal();
    }

    public void requireTeamAccess(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        if (!user.hasTeamAccess(teamId)) {
            throw new ForbiddenException("You are not a member of team " + teamId);
        }
    }

    public void requireRoleAtLeast(UUID teamId, TeamRole minimumRole) {
        UserPrincipal user = getCurrentUser();
        if (!user.hasTeamRoleAtLeast(teamId, minimumRole)) {
            throw new ForbiddenException("Requires role " + minimumRole + " or higher in team " + teamId);
        }
    }

    public boolean canViewTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamAccess(teamId);
    }

    public boolean canCreateInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.MEMBER);
    }

    public boolean canEditInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.MEMBER);
    }

    public boolean canClaimInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.MEMBER);
    }

    public boolean canCommentInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.MEMBER);
    }

    public boolean canAssignInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.LEAD);
    }

    public boolean canApproveInTeam(UUID teamId) {
        UserPrincipal user = getCurrentUser();
        return user.hasTeamRoleAtLeast(teamId, TeamRole.LEAD);
    }

    public void verifyAssigneeInTeam(UUID assigneeId, UUID teamId) {
        if (!teamMembershipRepository.existsByUserIdAndTeamId(assigneeId, teamId)) {
            throw new ForbiddenException("Assignee is not a member of the item's team");
        }
    }

    public Set<UUID> getCurrentUserTeamIds() {
        return getCurrentUser().getTeamIds();
    }
}
