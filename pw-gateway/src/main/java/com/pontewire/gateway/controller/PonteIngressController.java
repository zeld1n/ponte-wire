package com.pontewire.gateway.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import common.DTO.WebhookEvent;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/webhook")
@RequiredArgsConstructor
public class PonteIngressController {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final Tracer tracer;                      // Micrometer Tracer (backed by OTel)
    private final ObservationRegistry observationRegistry;


    @PostMapping("/{source}")
    public Mono<Void> ingest(@PathVariable String source,
                             @RequestBody Map<String, Object> payload) {

        log.info("[traceId visible in MDC] Routing webhook from source={}", source);
        WebhookEvent event = new WebhookEvent(source, payload, Instant.now());

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {
                    String topic = "pw.incoming." + source;
                    return Mono.fromFuture(kafkaTemplate.send(topic, UUID.randomUUID().toString(), json).toCompletableFuture());
                })
                .doOnSuccess(result -> log.info("Event published to Kafka, topic={}", result.getRecordMetadata().topic()))
                .then();
    }


    @PostMapping("/manual/{source}")
    public Mono<Void> sendWithExplicitTraceHeader(@PathVariable String source,
                                                  @RequestBody Map<String, Object> payload) {

        WebhookEvent event = new WebhookEvent(source, payload, Instant.now());

        return Mono.fromCallable(() -> objectMapper.writeValueAsString(event))
                .flatMap(json -> {

                    ProducerRecord<String, String> record =
                            new ProducerRecord<>("pw.incoming." + source, source, json);

                    Span currentSpan = tracer.currentSpan();
                    if (currentSpan != null) {
                        TraceContext ctx = currentSpan.context();

                        String traceparent = "00-" + ctx.traceId()
                                + "-" + ctx.spanId()
                                + "-01";

                        record.headers().add(
                                "traceparent",
                                traceparent.getBytes(StandardCharsets.UTF_8)
                        );
                        record.headers().add(
                                "uber-trace-id",
                                (ctx.traceId() + ":" + ctx.spanId() + ":0:1")
                                        .getBytes(StandardCharsets.UTF_8)
                        );
                        log.debug("Injected traceparent={} into Kafka record", traceparent);
                    }

                    return Mono.fromFuture(kafkaTemplate.send(record).toCompletableFuture());
                })
                .then();
    }
}