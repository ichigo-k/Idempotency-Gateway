package com.finsafe.idempotency_gateway.services.HashService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;

public interface HashService {
    String sha256(PaymentRequest input);
    boolean matchesSha256(PaymentRequest input, String expectedHash);
}
