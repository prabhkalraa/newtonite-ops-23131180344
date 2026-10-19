package com.newtonite.ops.outbox;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.outbox.enabled", havingValue = "true", matchIfMissing = true)
public class OutboxWorker {

    private static final Logger log = LoggerFactory.getLogger(OutboxWorker.class);

    private final OutboxJobRepository outboxJobRepository;
    private final OutboxService outboxService;
    private final int batchSize;

    public OutboxWorker(
            OutboxJobRepository outboxJobRepository,
            OutboxService outboxService,
            @Value("${app.outbox.batch-size:10}") int batchSize) {
        this.outboxJobRepository = outboxJobRepository;
        this.outboxService = outboxService;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${app.outbox.poll-interval-ms:3000}")
    public void processDueJobs() {
        Instant now = Instant.now();
        List<OutboxJob> dueJobs = outboxJobRepository.findDuePendingJobs(now, PageRequest.of(0, batchSize));

        if (dueJobs.isEmpty()) {
            return;
        }

        for (OutboxJob job : dueJobs) {
            // Atomic conditional update to claim exclusively and prevent multi-worker concurrency race
            int claimed = outboxJobRepository.claimJobAtomically(job.getId(), now);
            if (claimed > 0) {
                try {
                    outboxService.processJob(job.getId());
                } catch (Exception ex) {
                    log.error("Unexpected error processing outbox job [{}]: {}", job.getId(), ex.getMessage());
                }
            }
        }
    }
}
