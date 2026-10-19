package com.newtonite.ops.security;

import com.newtonite.ops.team.TeamMembership;
import com.newtonite.ops.team.TeamMembershipRepository;
import com.newtonite.ops.user.User;
import com.newtonite.ops.user.UserRepository;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final TeamMembershipRepository teamMembershipRepository;

    public CustomUserDetailsService(UserRepository userRepository, TeamMembershipRepository teamMembershipRepository) {
        this.userRepository = userRepository;
        this.teamMembershipRepository = teamMembershipRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserPrincipal loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        List<TeamMembership> memberships = teamMembershipRepository.findByUserIdWithTeam(user.getId());
        return new UserPrincipal(user, memberships);
    }
}
