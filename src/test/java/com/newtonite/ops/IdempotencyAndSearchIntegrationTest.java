package com.newtonite.ops;

import com.newtonite.ops.team.Team;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class IdempotencyAndSearchIntegrationTest extends BaseIntegrationTest {

    @Test
    @DisplayName("Requirement 14: Repeated idempotency key with same payload returns original result without duplicate creation")
    void testRepeatedIdempotencyKeyReturnsOriginalResult() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        String charlieToken = getBearerTokenForUser("member_charlie");

        String idempotencyKey = "key-" + UUID.randomUUID();

        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Idempotent Creation Test Item",
                "Testing replay of identical requests",
                WorkItemType.INCIDENT,
                WorkItemPriority.HIGH,
                coreInfra.getId(),
                null,
                null
        );

        long countBefore = workItemRepository.count();

        // 1. First execution
        MvcResult firstResult = mockMvc.perform(post("/api/items")
                        .header("Authorization", charlieToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andReturn();

        WorkItemResponse firstResponse = objectMapper.readValue(
                firstResult.getResponse().getContentAsString(), WorkItemResponse.class
        );

        // 2. Second execution (replay with identical key and payload)
        MvcResult secondResult = mockMvc.perform(post("/api/items")
                        .header("Authorization", charlieToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        WorkItemResponse secondResponse = objectMapper.readValue(
                secondResult.getResponse().getContentAsString(), WorkItemResponse.class
        );

        // Verify identical item ID returned and NO duplicate created
        assertEquals(firstResponse.getId(), secondResponse.getId(), "Replayed request must return the exact same item ID");
        long countAfter = workItemRepository.count();
        assertEquals(countBefore + 1, countAfter, "Database row count must only increase by 1");
    }

    @Test
    @DisplayName("Requirement 15: Reusing an idempotency key with a different payload is rejected with HTTP 409 Conflict")
    void testReusedKeyWithDifferentPayloadRejected() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        String charlieToken = getBearerTokenForUser("member_charlie");

        String idempotencyKey = "key-conflict-" + UUID.randomUUID();

        CreateWorkItemRequest originalRequest = new CreateWorkItemRequest(
                "Original Payload Title",
                "Original Description",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                coreInfra.getId(),
                null,
                null
        );

        // Initial creation
        mockMvc.perform(post("/api/items")
                        .header("Authorization", charlieToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(originalRequest)))
                .andExpect(status().isCreated());

        // Reusing the same key with DIFFERENT title
        CreateWorkItemRequest conflictingRequest = new CreateWorkItemRequest(
                "Completely Different Title",
                "Original Description",
                WorkItemType.TASK,
                WorkItemPriority.LOW,
                coreInfra.getId(),
                null,
                null
        );

        mockMvc.perform(post("/api/items")
                        .header("Authorization", charlieToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(conflictingRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("has already been used with a different request payload")));
    }

    @Test
    @DisplayName("Requirement 16: Server-side filters and pagination return the correct subset")
    void testServerSideFiltersAndPagination() throws Exception {
        String aliceToken = getBearerTokenForUser("admin_alice");

        // 1. Pagination: size = 5, page = 0
        mockMvc.perform(get("/api/items?page=0&size=5")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(lessThanOrEqualTo(5))))
                .andExpect(jsonPath("$.size", is(5)))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.totalElements", greaterThan(5)));

        // 2. Filter by status: OPEN
        mockMvc.perform(get("/api/items?status=OPEN")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[*].status", everyItem(is("OPEN"))));

        // 3. Filter by priority: URGENT
        mockMvc.perform(get("/api/items?priority=URGENT")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[*].priority", everyItem(is("URGENT"))));

        // 4. Free-text search matching "Kubernetes" or "Kafka"
        mockMvc.perform(get("/api/items?search=Kubernetes")
                        .header("Authorization", aliceToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.[0].title", containsStringIgnoringCase("Kubernetes")));
    }
}
