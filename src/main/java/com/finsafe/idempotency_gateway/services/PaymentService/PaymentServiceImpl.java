package com.finsafe.idempotency_gateway.services.PaymentService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class PaymentServiceImpl implements PaymentService {

    public PaymentResponse process(String key, String userId, PaymentRequest paymentRequest){
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }

        String message = "Charged " + paymentRequest.amount()+ " " + paymentRequest.currency();

        return new PaymentResponse(message);
    }
}
