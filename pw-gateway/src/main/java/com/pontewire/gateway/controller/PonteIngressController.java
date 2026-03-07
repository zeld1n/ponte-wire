package com.pontewire.gateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/v1/bridge")
public class PonteIngressController {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public PonteIngressController(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @PostMapping("/{key}")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public Mono<Void> ingestEvent(@PathVariable String key, @RequestBody String payload) {
        return Mono.fromRunnable(() ->
                kafkaTemplate.send("pw.incoming", key, payload)
        ).then();
    }
}