package com.finsafe.idempotency_gateway.services.PaymentService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import com.finsafe.idempotency_gateway.entities.Transaction;
import com.finsafe.idempotency_gateway.repositories.TransactionRepository;
import com.finsafe.idempotency_gateway.services.HashService.HashServiceimpl;
import com.finsafe.idempotency_gateway.services.IdempotencyService.IdempotencyServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final TransactionRepository transactionRepository;
    private final IdempotencyServiceImpl idempotencyService;
    private final HashServiceimpl hashService;

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    @Override
    public PaymentResponse process(String idempotencyKey, String clientId, PaymentRequest paymentRequest){

        String hash  = hashService.sha256(paymentRequest);
        Boolean hasStarted = idempotencyService.start(clientId, idempotencyKey, hash);
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Payment processing interrupted", e);
        }
        Transaction transaction = new Transaction(paymentRequest.amount(), paymentRequest.currency());

        log.info("Processing payment request");
        transactionRepository.save(transaction);
        log.info("Transaction {} saved to DB", transaction.getId());

        String message = "Charged " + paymentRequest.amount()+ " " + paymentRequest.currency();
        idempotencyService.complete(clientId, idempotencyKey, hash, 200, message);

        return new PaymentResponse(message);
    }
}
