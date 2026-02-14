package com.finsafe.idempotency_gateway.services.HashService;

import com.finsafe.idempotency_gateway.dtos.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Service
@RequiredArgsConstructor
public class HashServiceimpl implements HashService{

    private final ObjectMapper objectMapper;

    @Override
    public String sha256(PaymentRequest input) {
        try {

            String json = objectMapper.writeValueAsString(input);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(json.getBytes(StandardCharsets.UTF_8));


            StringBuilder hex = new StringBuilder(hashBytes.length * 2);
            for (byte b : hashBytes) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (Exception e) {
            throw new RuntimeException("Error hashing PaymentRequest", e);
        }
    }

    @Override
    public boolean matchesSha256(PaymentRequest input, String expectedHash) {
        if (expectedHash == null) return false;
        return sha256(input).equalsIgnoreCase(expectedHash.trim());
    }
}

