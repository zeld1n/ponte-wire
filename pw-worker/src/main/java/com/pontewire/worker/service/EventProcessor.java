package com.pontewire.worker.service;

import common.DTO.WebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pontewire.worker.entity.ProcessedEvent;
import com.pontewire.worker.repository.EventRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventProcessor {

    private final EventRepository repository;
    private final ObjectMapper objectMapper;
    private final MeterRegistry meterRegistry;

    @KafkaListener(topics = "pw.incoming", groupId = "pw-worker-group")
    public void process(String message) throws Exception {
        // Convert the incoming JSON string into a WebhookEvent object
        WebhookEvent event = objectMapper.readValue(message, WebhookEvent.class);
        log.info("Event decoded from source: {}", event.source());

        String jsonPayload = objectMapper.writeValueAsString(event.data());

        ProcessedEvent entity = ProcessedEvent.builder()
                .source(event.source())
                .payload(jsonPayload)
                .receivedAt(event.timestamp())
                .build();

        repository.save(entity)
                .doOnSuccess(saved -> {
                    log.info("Event saved to DB from source: {}", event.source());
                    meterRegistry.counter("pontewire.events.processed",
                            "source", event.source()).increment();
                })
                .doOnError(e -> {
                    log.error("Failed to save event from source: {}", event.source(), e);
                    meterRegistry.counter("pontewire.events.failed",
                            "source", event.source()).increment();
                })
                .subscribe();
    }
}