package com.newtonite.ops.auth;

import com.newtonite.ops.security.AuthorizationService;
import com.newtonite.ops.security.CustomUserDetailsService;
import com.newtonite.ops.security.JwtService;
import com.newtonite.ops.security.UserPrincipal;
import com.newtonite.ops.team.TeamMembership;
import com.newtonite.ops.team.TeamMembershipRepository;
import com.newtonite.ops.team.TeamResponse;
import com.newtonite.ops.user.UserResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final TeamMembershipRepository teamMembershipRepository;
    private final AuthorizationService authorizationService;

    public AuthService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            CustomUserDetailsService userDetailsService,
            TeamMembershipRepository teamMembershipRepository,
            AuthorizationService authorizationService) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
        this.teamMembershipRepository = teamMembershipRepository;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public AuthResponse login(AuthRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        String token = jwtService.generateToken(principal);

        List<TeamMembership> memberships = teamMembershipRepository.findByUserIdWithTeam(principal.getId());
        List<TeamResponse> teams = memberships.stream()
                .map(tm -> TeamResponse.fromEntity(tm.getTeam(), tm.getRole()))
                .collect(Collectors.toList());

        UserResponse userResponse = new UserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getEmail()
        );

        return new AuthResponse(token, userResponse, teams);
    }

    @Transactional(readOnly = true)
    public CurrentUserResponse getCurrentUser() {
        UserPrincipal principal = authorizationService.getCurrentUser();
        List<TeamMembership> memberships = teamMembershipRepository.findByUserIdWithTeam(principal.getId());
        List<TeamResponse> teams = memberships.stream()
                .map(tm -> TeamResponse.fromEntity(tm.getTeam(), tm.getRole()))
                .collect(Collectors.toList());

        UserResponse userResponse = new UserResponse(
                principal.getId(),
                principal.getUsername(),
                principal.getDisplayName(),
                principal.getEmail()
        );

        return new CurrentUserResponse(userResponse, teams);
    }
}
