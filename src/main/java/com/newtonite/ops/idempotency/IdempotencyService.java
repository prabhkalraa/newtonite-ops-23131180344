package com.newtonite.ops.idempotency;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtonite.ops.exception.ConflictException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class IdempotencyService {

    private final IdempotencyRepository idempotencyRepository;
    private final ObjectMapper objectMapper;

    public IdempotencyService(IdempotencyRepository idempotencyRepository, ObjectMapper objectMapper) {
        this.idempotencyRepository = idempotencyRepository;
        this.objectMapper = objectMapper;
    }

    public String computeHash(Object requestPayload) {
        try {
            String json = objectMapper.writeValueAsString(requestPayload);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to compute request hash for idempotency", e);
        }
    }

    @Transactional(readOnly = true)
    public Optional<IdempotencyRecord> findExisting(String key, UUID actorId, String operation, String incomingHash) {
        Optional<IdempotencyRecord> recordOpt = idempotencyRepository
                .findByIdempotencyKeyAndActorIdAndOperation(key, actorId, operation);

        if (recordOpt.isPresent()) {
            IdempotencyRecord record = recordOpt.get();
            if (!record.getRequestHash().equals(incomingHash)) {
                throw new ConflictException("Idempotency key '" + key + "' has already been used with a different request payload.");
            }
            return Optional.of(record);
        }
        return Optional.empty();
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public IdempotencyRecord saveRecord(String key, UUID actorId, String operation,
                                        String requestHash, int status, String responseBody) {
        IdempotencyRecord record = new IdempotencyRecord(
                UUID.randomUUID(),
                key,
                actorId,
                operation,
                requestHash,
                status,
                responseBody
        );
        return idempotencyRepository.save(record);
    }
}
