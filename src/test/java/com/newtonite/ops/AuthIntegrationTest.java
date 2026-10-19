package com.newtonite.ops;

import com.newtonite.ops.auth.AuthRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class AuthIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Requirement 1: Successful login returns 200 OK and valid JWT token")
    void testSuccessfulLogin() throws Exception {
        AuthRequest request = new AuthRequest("admin_alice", "Password123!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token", notNullValue()))
                .andExpect(jsonPath("$.tokenType", is("Bearer")))
                .andExpect(jsonPath("$.user.username", is("admin_alice")))
                .andExpect(jsonPath("$.teams", not(empty())));
    }

    @Test
    @DisplayName("Requirement 2: Invalid login credentials are rejected with HTTP 401 Unauthorized")
    void testInvalidLogin() throws Exception {
        AuthRequest request = new AuthRequest("admin_alice", "WrongPassword!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code", is("INVALID_CREDENTIALS")))
                .andExpect(jsonPath("$.message", containsString("Invalid username or password")));
    }

    @Test
    @DisplayName("Requirement 3: Unauthenticated API access is rejected")
    void testUnauthenticatedAccessRejected() throws Exception {
        mockMvc.perform(get("/api/items"))
                .andExpect(status().isForbidden());
    }
}
