package com.newtonite.ops.auth;

import com.newtonite.ops.team.TeamResponse;
import com.newtonite.ops.user.UserResponse;

import java.util.List;

public class AuthResponse {
    private String token;
    private String tokenType = "Bearer";
    private UserResponse user;
    private List<TeamResponse> teams;

    public AuthResponse() {}

    public AuthResponse(String token, UserResponse user, List<TeamResponse> teams) {
        this.token = token;
        this.user = user;
        this.teams = teams;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
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
