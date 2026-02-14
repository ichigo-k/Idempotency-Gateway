package com.finsafe.idempotency_gateway.controllers;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import com.finsafe.idempotency_gateway.models.IdempotencyRecord;
import com.finsafe.idempotency_gateway.services.IdempotencyService.IdempotencyServiceImpl;
import com.finsafe.idempotency_gateway.services.PaymentService.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

;
import java.util.Optional;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/")
public class PaymentController {

    private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    private  static final String CLIENT_ID_HEADER = "Client-Id";

    private final PaymentServiceImpl paymentService;
    private final IdempotencyServiceImpl idempotencyService;


    @PostMapping("process-payment")
    public ResponseEntity<PaymentResponse> process_payment(@RequestHeader(value = IDEMPOTENCY_KEY_HEADER) String idempotencyKey, @RequestHeader(value = CLIENT_ID_HEADER) String clientId, @Valid @RequestBody PaymentRequest request
    ){

        Optional<IdempotencyRecord> rec = idempotencyService.get(clientId, idempotencyKey);

        if(rec.isPresent()){
            return ResponseEntity
                    .status(rec.get().getHttpStatus())
                    .header("X-Cache-Hit", "true")
                    .body(new PaymentResponse(rec.get().getResponseBody()));
        }
        PaymentResponse results = paymentService.process(idempotencyKey, clientId, request);
        return ResponseEntity.ok(results);
    }

}
