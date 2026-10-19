package com.newtonite.ops.team;

import com.newtonite.ops.security.AuthorizationService;
import com.newtonite.ops.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class TeamService {

    private final TeamMembershipRepository teamMembershipRepository;
    private final AuthorizationService authorizationService;

    public TeamService(
            TeamMembershipRepository teamMembershipRepository,
            AuthorizationService authorizationService) {
        this.teamMembershipRepository = teamMembershipRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public List<TeamResponse> getMyTeams() {
        UserPrincipal principal = authorizationService.getCurrentUser();
        List<TeamMembership> memberships = teamMembershipRepository.findByUserIdWithTeam(principal.getId());
        return memberships.stream()
                .map(tm -> TeamResponse.fromEntity(tm.getTeam(), tm.getRole()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TeamMemberResponse> getTeamMembers(UUID teamId) {
        authorizationService.requireTeamAccess(teamId);
        List<TeamMembership> memberships = teamMembershipRepository.findByTeamIdWithUser(teamId);
        return memberships.stream()
                .map(TeamMemberResponse::fromMembership)
                .collect(Collectors.toList());
    }
}
