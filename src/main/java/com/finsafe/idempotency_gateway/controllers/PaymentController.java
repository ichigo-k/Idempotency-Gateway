package com.finsafe.idempotency_gateway.controllers;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import com.finsafe.idempotency_gateway.dtos.PaymentResponse;
import com.finsafe.idempotency_gateway.services.PaymentService.PaymentServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/v1/")
public class PaymentController {

    public static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
    public static final String USER_ID_HEADER = "UserId";

    public final PaymentServiceImpl paymentService;

    @PostMapping("process-payment")
    public ResponseEntity<String> process_payment(
            @RequestHeader(value = IDEMPOTENCY_KEY_HEADER) String idempotencyKey,
            @RequestHeader(value = USER_ID_HEADER) String userId,
            @Valid @RequestBody PaymentRequest request
    ){
        PaymentResponse results = paymentService.process(idempotencyKey, userId, request);


        return new ResponseEntity<>(results.message(),HttpStatus.OK);
    }

}
