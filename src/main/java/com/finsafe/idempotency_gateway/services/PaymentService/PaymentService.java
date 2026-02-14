package com.finsafe.idempotency_gateway.services.PaymentService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;

public interface PaymentService {
    PaymentResponse process(String key, String clientId, PaymentRequest paymentRequest);
}
