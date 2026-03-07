package com.pontewire.worker.repository;

import com.pontewire.worker.entity.ProcessedEvent;
import com.pontewire.worker.service.EventProcessor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface EventRepository extends ReactiveCrudRepository<ProcessedEvent, Long> {

}

