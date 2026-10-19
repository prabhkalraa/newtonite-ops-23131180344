package com.newtonite.ops.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class OutboxEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OutboxEventHandler.class);
    private final ObjectMapper objectMapper;
    // In-memory set to ensure idempotency for demonstration (can be extended with DB table if needed)
    private final Set<String> processedEvents = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public OutboxEventHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void handle(OutboxJob job) throws Exception {
        String eventKey = job.getId().toString();
        if (processedEvents.contains(eventKey)) {
            log.info("Outbox event [{}] already processed idempotently. Skipping.", eventKey);
            return;
        }

        JsonNode payload = objectMapper.readTree(job.getPayload());

        switch (job.getEventType()) {
            case "WORK_ITEM_CREATED" -> {
                String title = payload.path("title").asText();
                String itemId = payload.path("itemId").asText();
                log.info("🔔 [NOTIFICATION] New work item created: '{}' (ID: {})", title, itemId);
            }
            case "WORK_ITEM_ASSIGNED" -> {
                String assignee = payload.path("assignee").asText();
                String title = payload.path("title").asText();
                log.info("🔔 [NOTIFICATION] Work item '{}' assigned to {}", title, assignee);
            }
            case "WORK_ITEM_STATUS_CHANGED" -> {
                String oldStatus = payload.path("oldStatus").asText();
                String newStatus = payload.path("newStatus").asText();
                String title = payload.path("title").asText();
                log.info("🔔 [NOTIFICATION] Work item '{}' status transitioned from {} to {}", title, oldStatus, newStatus);
            }
            default -> log.info("🔔 [NOTIFICATION] Generic event handled: {}", job.getEventType());
        }

        processedEvents.add(eventKey);
    }
}
