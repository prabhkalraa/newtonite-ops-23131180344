package com.newtonite.ops.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
public class OutboxService {

    private static final Logger log = LoggerFactory.getLogger(OutboxService.class);

    private final OutboxJobRepository outboxJobRepository;
    private final OutboxEventHandler eventHandler;
    private final ObjectMapper objectMapper;
    private final int maxAttempts;

    public OutboxService(
            OutboxJobRepository outboxJobRepository,
            OutboxEventHandler eventHandler,
            ObjectMapper objectMapper,
            @Value("${app.outbox.max-attempts:5}") int maxAttempts) {
        this.outboxJobRepository = outboxJobRepository;
        this.eventHandler = eventHandler;
        this.objectMapper = objectMapper;
        this.maxAttempts = maxAttempts;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public OutboxJob publishEvent(String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            OutboxJob job = new OutboxJob(UUID.randomUUID(), eventType, json);
            return outboxJobRepository.save(job);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize outbox event payload", e);
        }
    }

    @Transactional
    public void processJob(UUID jobId) {
        OutboxJob job = outboxJobRepository.findById(jobId).orElse(null);
        if (job == null) {
            return;
        }

        try {
            eventHandler.handle(job);
            job.setStatus(OutboxStatus.COMPLETED);
            job.setUpdatedAt(Instant.now());
            job.setLastError(null);
            outboxJobRepository.save(job);
            log.info("Outbox job [{}] of type {} COMPLETED successfully", job.getId(), job.getEventType());
        } catch (Exception ex) {
            int newAttempts = job.getAttempts() + 1;
            job.setAttempts(newAttempts);
            job.setLastError(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
            job.setUpdatedAt(Instant.now());

            if (newAttempts >= maxAttempts) {
                job.setStatus(OutboxStatus.FAILED);
                log.error("Outbox job [{}] FAILED permanently after {} attempts. Error: {}",
                        job.getId(), newAttempts, job.getLastError());
            } else {
                job.setStatus(OutboxStatus.PENDING);
                // Bounded exponential backoff: 2^attempts * 2 seconds (capped at 1 hour)
                long backoffSeconds = Math.min(3600, (long) Math.pow(2, newAttempts) * 2);
                job.setNextAttemptAt(Instant.now().plus(Duration.ofSeconds(backoffSeconds)));
                log.warn("Outbox job [{}] attempt {} failed. Retrying at {}. Error: {}",
                        job.getId(), newAttempts, job.getNextAttemptAt(), job.getLastError());
            }
            outboxJobRepository.save(job);
        }
    }
}
