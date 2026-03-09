package com.pontewire.gateway.controller;

import common.DTO.WebhookEvent;
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
@RequiredArgsConstructor
public class PonteIngressController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final ObjectMapper objectMapper;

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