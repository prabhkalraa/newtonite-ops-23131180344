
package com.newtonite.ops.security;

import com.newtonite.ops.team.TeamMembership;
import com.newtonite.ops.team.TeamRole;
import com.newtonite.ops.user.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String username;
    private final String displayName;
    private final String email;
    private final String password;
    private final boolean enabled;
    private final Map<UUID, TeamRole> teamRoles;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(User user, List<TeamMembership> memberships) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.displayName = user.getDisplayName();
        this.email = user.getEmail();
        this.password = user.getPasswordHash();
        this.enabled = user.isEnabled();

        Map<UUID, TeamRole> roles = new HashMap<>();
        List<GrantedAuthority> auths = new ArrayList<>();
        auths.add(new SimpleGrantedAuthority("ROLE_USER"));

        if (memberships != null) {
            for (TeamMembership tm : memberships) {
                roles.put(tm.getTeam().getId(), tm.getRole());
                auths.add(new SimpleGrantedAuthority("ROLE_" + tm.getRole().name()));
            }
        }
        this.teamRoles = Collections.unmodifiableMap(roles);
        this.authorities = Collections.unmodifiableList(auths);
    }

    public UUID getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getEmail() {
        return email;
    }

    public Map<UUID, TeamRole> getTeamRoles() {
        return teamRoles;
    }

    public Optional<TeamRole> getRoleForTeam(UUID teamId) {
        return Optional.ofNullable(teamRoles.get(teamId));
    }

    public boolean hasTeamAccess(UUID teamId) {
        return teamRoles.containsKey(teamId);
    }

    public boolean hasTeamRoleAtLeast(UUID teamId, TeamRole minimumRole) {
        TeamRole role = teamRoles.get(teamId);
        return role != null && role.isAtLeast(minimumRole);
    }

    public Set<UUID> getTeamIds() {
        return teamRoles.keySet();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
