package com.newtonite.ops.team;

public enum TeamRole {
    VIEWER,
    MEMBER,
    LEAD,
    ADMIN;

    public boolean isAtLeast(TeamRole minimumRole) {
        return this.ordinal() >= minimumRole.ordinal();
    }
}
