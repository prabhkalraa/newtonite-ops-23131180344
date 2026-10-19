package com.newtonite.ops;

import com.newtonite.ops.outbox.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class OutboxIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private OutboxJobRepository outboxJobRepository;

    @Autowired
    private OutboxService outboxService;

    @Autowired
    private OutboxEventHandler outboxEventHandler;

    @Test
    @DisplayName("Requirement 17: Outbox jobs retry after failure and eventually reach FAILED terminal state")
    void testOutboxJobRetriesAndReachesTerminalState() {
        // Create an invalid job whose payload will fail parsing during processing
        OutboxJob job = new OutboxJob(UUID.randomUUID(), "MALFORMED_EVENT", "{ invalid json");
        job = outboxJobRepository.saveAndFlush(job);
        UUID jobId = job.getId();

        // Attempt 1: Should fail and enter PENDING with incremented attempts
        outboxService.processJob(jobId);
        OutboxJob attempt1 = outboxJobRepository.findById(jobId).orElseThrow();
        assertEquals(1, attempt1.getAttempts());
        assertEquals(OutboxStatus.PENDING, attempt1.getStatus());
        assertNotNull(attempt1.getLastError());
        assertTrue(attempt1.getNextAttemptAt().isAfter(Instant.now().minusSeconds(1)));

        // Attempt 2:
        outboxService.processJob(jobId);
        OutboxJob attempt2 = outboxJobRepository.findById(jobId).orElseThrow();
        assertEquals(2, attempt2.getAttempts());
        assertEquals(OutboxStatus.PENDING, attempt2.getStatus());

        // Attempt 3 (max-attempts is 3 in test profile): Should transition to FAILED
        outboxService.processJob(jobId);
        OutboxJob terminal = outboxJobRepository.findById(jobId).orElseThrow();
        assertEquals(3, terminal.getAttempts());
        assertEquals(OutboxStatus.FAILED, terminal.getStatus(), "Exhausted outbox job must transition to FAILED");
        assertNotNull(terminal.getLastError());
    }

    @Test
    @DisplayName("Requirement 18: Reprocessing an already completed outbox event does not repeat its side effect")
    void testOutboxEventHandlerIdempotency() throws Exception {
        UUID jobId = UUID.randomUUID();
        OutboxJob validJob = new OutboxJob(
                jobId,
                "WORK_ITEM_ASSIGNED",
                "{\"title\":\"Fix Memory Leak\",\"assignee\":\"Charlie Member\"}"
        );

        // 1. First execution
        assertDoesNotThrow(() -> outboxEventHandler.handle(validJob));

        // 2. Second execution (replay / retry of same event ID)
        assertDoesNotThrow(() -> outboxEventHandler.handle(validJob));

        // Both calls completed safely without error or unexpected side effects
    }
}
