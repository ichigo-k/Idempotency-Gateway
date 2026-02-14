package com.finsafe.idempotency_gateway.models;

import com.finsafe.idempotency_gateway.enums.OpStatus;
import lombok.Data;

import java.time.Instant;

@Data
public class IdempotencyRecord {
    private String requestHash;
    private OpStatus status;
    private int httpStatus;
    private String responseBody;
    private Instant createdAt;
    private Instant completedAt;
}
