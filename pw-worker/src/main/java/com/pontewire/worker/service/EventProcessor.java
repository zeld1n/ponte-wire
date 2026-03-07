package com.pontewire.worker.service;

import com.pontewire.worker.entity.ProcessedEvent;
import com.pontewire.worker.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventProcessor {

    private final EventRepository repository;


    @KafkaListener(topics = "pw.incoming")
    public void process(ConsumerRecord<String,String> record) {
        log.info("Received event: {} (Key: {})", record.value(), record.key());

        ProcessedEvent event = ProcessedEvent.builder()
                .source(record.key())
                .payload(record.value())
                .receivedAt(LocalDateTime.now())
                .build();

        repository.save(event)
                .doOnSuccess(saved -> log.info("saved to db : {}", saved.getId()))
                .subscribe(); // subscribe worker for execute


    }




}
