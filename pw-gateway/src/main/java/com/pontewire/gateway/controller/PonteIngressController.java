package com.pontewire.gateway.controller;

import common.DTO.WebhookEvent;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

@RestController
@RequestMapping("/api/v1/bridge")
public class PonteIngressController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

    private final MeterRegistry meterRegistry;

    public PonteIngressController(KafkaTemplate<String, String> kafkaTemplate, ObjectMapper objectMapper, MeterRegistry meterRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.meterRegistry = meterRegistry;
    }

    @PostMapping("/{source}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingestEvent(
            @PathVariable String source,
            @Valid @RequestBody WebhookEvent event) {

        WebhookEvent normalizedEvent = new WebhookEvent(
                source,
                event.data(),
                event.timestamp()
        );
        meterRegistry.counter("pontewire.webhooks.received",
                "source", source).increment();

        return Mono.fromRunnable(() -> sendToKafka(source, normalizedEvent)).then();
    }


    @SneakyThrows
    private void sendToKafka(String source, WebhookEvent event) {
        // Conv onj webhookevent -> JSON
        String payload = objectMapper.writeValueAsString(event);

        //source -> key ; event -> paylod
        kafkaTemplate.send("pw.incoming", source, payload);
    }
}