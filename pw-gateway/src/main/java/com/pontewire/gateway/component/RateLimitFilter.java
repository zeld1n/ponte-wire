package com.pontewire.gateway.component;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@Order(2)
@RequiredArgsConstructor
public class RateLimitFilter implements WebFilter {

    private final ReactiveRedisTemplate<String, String> redisTemplate;
    private final MeterRegistry meterRegistry;

    @Value("${pontewire.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();


        if (path.startsWith("/actuator")) {
            return chain.filter(exchange);
        }

        // Retrive tenant from URL: /api/v1/bridge/stripe → stripe
        String tenant = extractTenant(path);
        String redisKey = "rate-limit:" + tenant;

        return incrementCounter(redisKey)
                .flatMap(count -> handleRequest(count, tenant, exchange, chain));
    }

    // Increment and start Time to live
    private Mono<Long> incrementCounter(String redisKey) {
        return redisTemplate.opsForValue()
                .increment(redisKey)
                .doOnNext(count -> {
                    if (count == 1) {
                        redisTemplate.expire(redisKey, Duration.ofMinutes(1)).subscribe();
                    }
                });
    }

    // step 2 -> allow or deny
    private Mono<Void> handleRequest(Long count, String tenant,
                                     ServerWebExchange exchange,
                                     WebFilterChain chain) {
        log.debug("Rate limit: tenant={}, count={}/{}", tenant, count, requestsPerMinute);

        if (count > requestsPerMinute) {
            return rejectRequest(tenant, exchange);
        }

        return allowRequest(count, exchange, chain);
    }

    // Reject Request 429
    private Mono<Void> rejectRequest(String tenant, ServerWebExchange exchange) {
        log.warn("Rate limit exceeded for tenant: {}", tenant);

        meterRegistry.counter("pontewire.rate.limit.exceeded",
                "tenant", tenant).increment();

        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Retry-After", "60");
        exchange.getResponse().getHeaders().add("X-RateLimit-Remaining", "0");
        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Limit", String.valueOf(requestsPerMinute));

        return exchange.getResponse().setComplete();
    }

    // Allow Request
    private Mono<Void> allowRequest(Long count, ServerWebExchange exchange,
                                    WebFilterChain chain) {
        long remaining = requestsPerMinute - count;

        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        exchange.getResponse().getHeaders()
                .add("X-RateLimit-Remaining", String.valueOf(remaining));

        return chain.filter(exchange);
    }

    private String extractTenant(String path) {
        // /api/v1/bridge/stripe → stripe
        String[] parts = path.split("/");
        return parts.length > 4 ? parts[4] : "unknown";
    }
}