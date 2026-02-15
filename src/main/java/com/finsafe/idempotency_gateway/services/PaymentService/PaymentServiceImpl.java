package com.finsafe.idempotency_gateway.services.PaymentService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import com.finsafe.idempotency_gateway.entities.Transaction;
import com.finsafe.idempotency_gateway.enums.OpStatus;
import com.finsafe.idempotency_gateway.models.IdempotencyRecord;
import com.finsafe.idempotency_gateway.repositories.TransactionRepository;
import com.finsafe.idempotency_gateway.services.HashService.HashServiceimpl;
import com.finsafe.idempotency_gateway.services.IdempotencyService.IdempotencyServiceImpl;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final TransactionRepository transactionRepository;
    private final IdempotencyServiceImpl idempotencyService;
    private final HashServiceimpl hashService;

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);
    private static final int MAX_RETRIES = 3;

    @Override
    public ResponseEntity<PaymentResponse> process(String idempotencyKey, String clientId, PaymentRequest paymentRequest) throws InterruptedException
    {

        String hash = hashService.sha256(paymentRequest);

        boolean started = idempotencyService.start(clientId, idempotencyKey, hash);


        if (!started) {
            Optional<IdempotencyRecord> recOpt = idempotencyService.get(clientId, idempotencyKey);

            if (recOpt.isEmpty()) {
                return ResponseEntity
                        .status(HttpStatus.ACCEPTED)
                        .body(new PaymentResponse("Unable to determine request state. Please retry."));
            }

            IdempotencyRecord rec = recOpt.get();


            if (!hashService.matchesSha256(paymentRequest,rec.getRequestHash())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .body(new PaymentResponse("Idempotency key already used for a different request body."));
            }

            if (rec.getStatus() == OpStatus.COMPLETED) {
                return ResponseEntity
                        .status(rec.getHttpStatus())
                        .header("X-Cache-Hit", "true")
                        .body(new PaymentResponse(rec.getResponseBody()));
            }


            int tried = 0;
            while (tried <  MAX_RETRIES) {
                Thread.sleep(2000);
                Optional<IdempotencyRecord> current = idempotencyService.get(clientId, idempotencyKey);

                if (current.isPresent() && current.get().getStatus() == OpStatus.COMPLETED) {
                    IdempotencyRecord done = current.get();
                    return ResponseEntity
                            .status(done.getHttpStatus())
                            .header("X-Cache-Hit", "true")
                            .body(new PaymentResponse(done.getResponseBody()));
                }

                tried++;
            }


            return ResponseEntity
                    .status(HttpStatus.ACCEPTED)
                    .body(new PaymentResponse("Request is still being processed. Retry shortly."));
        }


        try {
            TimeUnit.SECONDS.sleep(2);

            log.info("Processing payment request");
            Transaction tx = new Transaction(paymentRequest.amount(), paymentRequest.currency());
            transactionRepository.save(tx);
            log.info("Transaction {} saved to DB", tx.getId());

            String message = "Charged " + paymentRequest.amount() + " " + paymentRequest.currency();

            int status = HttpStatus.CREATED.value();
            idempotencyService.complete(clientId, idempotencyKey, hash, status, message);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(new PaymentResponse(message));

        } catch (RuntimeException ex) {
            throw ex;
        }
    }

}
