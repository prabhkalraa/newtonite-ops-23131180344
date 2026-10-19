package com.newtonite.ops.team;

import com.newtonite.ops.user.User;
import java.util.UUID;

public class TeamMemberResponse {
    private UUID userId;
    private String username;
    private String displayName;
    private String email;
    private TeamRole role;

    public TeamMemberResponse() {}

    public TeamMemberResponse(UUID userId, String username, String displayName, String email, TeamRole role) {
        this.userId = userId;
        this.username = username;
        this.displayName = displayName;
        this.email = email;
        this.role = role;
    }

    public static TeamMemberResponse fromMembership(TeamMembership membership) {
        if (membership == null) return null;
        User u = membership.getUser();
        return new TeamMemberResponse(
                u.getId(),
                u.getUsername(),
                u.getDisplayName(),
                u.getEmail(),
                membership.getRole()
        );
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public TeamRole getRole() {
        return role;
    }

    public void setRole(TeamRole role) {
        this.role = role;
    }
}
