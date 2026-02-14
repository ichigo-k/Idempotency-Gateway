package com.finsafe.idempotency_gateway.services.IdempotencyService;

import com.finsafe.idempotency_gateway.models.IdempotencyRecord;

import java.util.Optional;

public interface IdempotencyService {
    String buildKey(String clientId, String idemKey);

    Optional<IdempotencyRecord> get(String clientId, String idemKey);

    boolean start(String clientId, String idemKey, String requestHash);

    void complete(String clientId, String idemKey, String requestHash, int httpStatus, String responseBody);
}
