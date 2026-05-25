package com.pontewire.worker.service;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;

@Slf4j
@Service
@RequiredArgsConstructor
public class IdempotencyService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${pontewire.idempotency.ttl-hours:24}")
    private long ttlHours;


    public Mono<Boolean> isDuplicate(String payload) {
        String hash = sha256(payload);
        String key = "idempotency:" + hash;

        return redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofHours(ttlHours))
                .map(inserted -> {
                    if (Boolean.FALSE.equals(inserted)) {
                        log.warn("Duplicate event detected, hash={}", hash);
                        meterRegistry.counter("pontewire.duplicates.detected").increment();
                        return true;
                    }
                    return false;
                });
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}