package com.finsafe.idempotency_gateway.services.IdempotencyService;

import com.finsafe.idempotency_gateway.models.IdempotencyRecord;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import com.finsafe.idempotency_gateway.enums.OpStatus;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {

    private final RedisTemplate<String, Object> redis;
    private final ObjectMapper objectMapper;

    private final Duration ttl = Duration.ofMinutes(15);


    @Override
    public String buildKey(String clientId, String idemKey) {
        return "idem:" + clientId + ":" + idemKey;
    }

    @Override
    public Optional<IdempotencyRecord> get(String clientId, String idemKey) {
        Object val = redis.opsForValue().get(this.buildKey(clientId, idemKey));
        if (val == null) return Optional.empty();
        IdempotencyRecord rec = objectMapper.convertValue(val, IdempotencyRecord.class);
        return Optional.of(rec);
    }

    @Override
    public boolean start(String clientId, String idemKey, String requestHash) {
        IdempotencyRecord rec = new IdempotencyRecord();
        rec.setRequestHash(requestHash);
        rec.setStatus(OpStatus.IN_PROGRESS);
        rec.setCreatedAt(Instant.now());

        Boolean ok = redis.opsForValue().setIfAbsent(this.buildKey(clientId, idemKey), rec, ttl);
        return Boolean.TRUE.equals(ok);
    }

    @Override
    public void complete(String clientId, String idemKey, String requestHash, int httpStatus,
                         String responseBody) {

        IdempotencyRecord rec = new IdempotencyRecord();
        rec.setRequestHash(requestHash);
        rec.setStatus(OpStatus.COMPLETED);
        rec.setHttpStatus(httpStatus);
        rec.setResponseBody(responseBody);
        rec.setCompletedAt(Instant.now());

        redis.opsForValue().set(this.buildKey(clientId, idemKey), rec, ttl);
    }

}
