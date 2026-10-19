package com.newtonite.ops.team;

import java.util.UUID;

public class TeamResponse {
    private UUID id;
    private String name;
    private String description;
    private TeamRole userRole; // Role of current user in this team, if applicable

    public TeamResponse() {}

    public TeamResponse(UUID id, String name, String description, TeamRole userRole) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.userRole = userRole;
    }

    public static TeamResponse fromEntity(Team team, TeamRole userRole) {
        if (team == null) return null;
        return new TeamResponse(team.getId(), team.getName(), team.getDescription(), userRole);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TeamRole getUserRole() {
        return userRole;
    }

    public void setUserRole(TeamRole userRole) {
        this.userRole = userRole;
    }
}
