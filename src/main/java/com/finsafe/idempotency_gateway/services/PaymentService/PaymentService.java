package com.finsafe.idempotency_gateway.services.PaymentService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import org.springframework.http.ResponseEntity;

public interface PaymentService {
    ResponseEntity<PaymentResponse> process(String key, String clientId, PaymentRequest paymentRequest) throws InterruptedException;
}
