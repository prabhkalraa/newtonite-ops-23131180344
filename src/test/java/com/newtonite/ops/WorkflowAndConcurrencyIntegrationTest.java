package com.newtonite.ops;

import com.newtonite.ops.activity.ActivityEvent;
import com.newtonite.ops.activity.ActivityEventRepository;
import com.newtonite.ops.activity.ActivityEventType;
import com.newtonite.ops.team.Team;
import com.newtonite.ops.user.User;
import com.newtonite.ops.workitem.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class WorkflowAndConcurrencyIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ActivityEventRepository activityEventRepository;

    @Autowired
    private WorkItemService workItemService;

    @Test
    @DisplayName("Requirement 9: Invalid workflow transitions are rejected with HTTP 409 Conflict")
    void testInvalidWorkflowTransitionRejected() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        // Item in OPEN status cannot jump straight to RESOLVED
        WorkItem openItem = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()) && w.getStatus() == WorkItemStatus.OPEN)
                .findFirst().orElseThrow();

        String bobToken = getBearerTokenForUser("lead_bob");

        TransitionWorkItemRequest invalidTransition = new TransitionWorkItemRequest(
                WorkItemStatus.RESOLVED,
                openItem.getVersion()
        );

        mockMvc.perform(post("/api/items/" + openItem.getId() + "/transition")
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidTransition)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("Invalid workflow transition")));
    }

    @Test
    @DisplayName("Requirement 10: Successful creation records an activity event in the same transaction")
    void testCreationRecordsActivityEvent() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        String charlieToken = getBearerTokenForUser("member_charlie");

        CreateWorkItemRequest request = new CreateWorkItemRequest(
                "Audit Log Verification Test Item",
                "Ensure transactional activity history is written",
                WorkItemType.INCIDENT,
                WorkItemPriority.HIGH,
                coreInfra.getId(),
                null,
                null
        );

        MvcResult result = mockMvc.perform(post("/api/items")
                        .header("Authorization", charlieToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        WorkItemResponse response = objectMapper.readValue(result.getResponse().getContentAsString(), WorkItemResponse.class);

        // Verify Activity Event in DB
        List<ActivityEvent> events = activityEventRepository.findByWorkItemIdOrderByCreatedAtDesc(response.getId());
        assertFalse(events.isEmpty(), "Activity event must be recorded on creation");
        assertEquals(ActivityEventType.CREATED, events.get(0).getEventType());
        assertEquals("Charlie Member", events.get(0).getActorName());
    }

    @Test
    @DisplayName("Requirement 11: A failed business transaction does not leave an orphan activity event")
    void testFailedTransactionRollsBackActivityEvent() {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        User charlie = userRepository.findByUsername("member_charlie").orElseThrow();

        // Count existing activity events
        long initialEventCount = activityEventRepository.count();

        // Attempt an invalid update directly that causes an exception
        UpdateWorkItemRequest badRequest = new UpdateWorkItemRequest(
                "New Title",
                "Description",
                WorkItemPriority.HIGH,
                null,
                999999L // Wrong version guaranteed to throw ConflictException
        );

        WorkItem item = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()))
                .findFirst().orElseThrow();

        assertThrows(Exception.class, () -> {
            workItemService.updateWorkItem(item.getId(), badRequest);
        });

        // Ensure no extra activity event was committed
        long finalEventCount = activityEventRepository.count();
        assertEquals(initialEventCount, finalEventCount, "No orphan activity event should exist after transaction failure");
    }

    @Test
    @DisplayName("Requirement 12: Exactly one of two concurrent claims succeeds")
    void testConcurrentClaims() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        User alice = userRepository.findByUsername("admin_alice").orElseThrow();

        // Create a fresh unassigned item specifically for this concurrency test
        WorkItem unassignedItem = new WorkItem(
                UUID.randomUUID(),
                "Concurrent Claim Target",
                "Testing atomic conditional claim under pressure",
                WorkItemType.INCIDENT,
                WorkItemPriority.URGENT,
                WorkItemStatus.OPEN,
                coreInfra,
                null,
                alice,
                null
        );
        unassignedItem = workItemRepository.saveAndFlush(unassignedItem);
        UUID itemId = unassignedItem.getId();

        String bobToken = getBearerTokenForUser("lead_bob");
        String charlieToken = getBearerTokenForUser("member_charlie");

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);

        Callable<Void> task1 = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                mockMvc.perform(post("/api/items/" + itemId + "/claim")
                                .header("Authorization", bobToken))
                        .andDo(res -> {
                            int status = res.getResponse().getStatus();
                            if (status == 200) successCount.incrementAndGet();
                            else if (status == 409) conflictCount.incrementAndGet();
                        });
            } catch (Exception e) {
                // ignore
            }
            return null;
        };

        Callable<Void> task2 = () -> {
            readyLatch.countDown();
            startLatch.await();
            try {
                mockMvc.perform(post("/api/items/" + itemId + "/claim")
                                .header("Authorization", charlieToken))
                        .andDo(res -> {
                            int status = res.getResponse().getStatus();
                            if (status == 200) successCount.incrementAndGet();
                            else if (status == 409) conflictCount.incrementAndGet();
                        });
            } catch (Exception e) {
                // ignore
            }
            return null;
        };

        Future<Void> f1 = executor.submit(task1);
        Future<Void> f2 = executor.submit(task2);

        readyLatch.await(5, TimeUnit.SECONDS);
        startLatch.countDown(); // Fire both threads simultaneously

        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Verify that exactly ONE claim succeeded and exactly ONE failed with 409 Conflict
        assertEquals(1, successCount.get(), "Exactly one concurrent claim must succeed with HTTP 200");
        assertEquals(1, conflictCount.get(), "The competing concurrent claim must be rejected with HTTP 409");

        // Verify item in DB is now assigned
        WorkItem claimedItem = workItemRepository.findById(itemId).orElseThrow();
        assertNotNull(claimedItem.getAssignee(), "Claimed item must have an assignee");
    }

    @Test
    @DisplayName("Requirement 13: Stale-version update returns HTTP 409 Conflict")
    void testStaleVersionUpdateReturnsConflict() throws Exception {
        Team coreInfra = teamRepository.findByName("Core Infrastructure").orElseThrow();
        WorkItem item = workItemRepository.findAll().stream()
                .filter(w -> w.getTeam().getId().equals(coreInfra.getId()) && w.getStatus() != WorkItemStatus.CLOSED)
                .findFirst().orElseThrow();

        String bobToken = getBearerTokenForUser("lead_bob");

        // Client submits an outdated/stale version (currentVersion - 1)
        long staleVersion = item.getVersion() > 0 ? item.getVersion() - 1 : 9999L;

        UpdateWorkItemRequest staleRequest = new UpdateWorkItemRequest(
                "Attempted Stale Update",
                "Should be rejected",
                WorkItemPriority.LOW,
                null,
                staleVersion
        );

        mockMvc.perform(patch("/api/items/" + item.getId())
                        .header("Authorization", bobToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(staleRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONFLICT")))
                .andExpect(jsonPath("$.message", containsString("This item has changed since you opened it. Reload the latest version before editing.")));
    }
}
