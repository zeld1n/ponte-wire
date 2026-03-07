package com.pontewire.worker.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("processed_events")
public class ProcessedEvent {
    @Id
    private Long id;
    private String source;
    private String payload;
    private LocalDateTime receivedAt;
}