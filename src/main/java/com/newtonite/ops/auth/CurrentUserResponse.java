package com.newtonite.ops.auth;

import com.newtonite.ops.team.TeamResponse;
import com.newtonite.ops.user.UserResponse;

import java.util.List;

public class CurrentUserResponse {
    private UserResponse user;
    private List<TeamResponse> teams;

    public CurrentUserResponse() {}

    public CurrentUserResponse(UserResponse user, List<TeamResponse> teams) {
        this.user = user;
        this.teams = teams;
    }

    public UserResponse getUser() {
        return user;
    }

    public void setUser(UserResponse user) {
        this.user = user;
    }

    public List<TeamResponse> getTeams() {
        return teams;
    }

    public void setTeams(List<TeamResponse> teams) {
        this.teams = teams;
    }
}
