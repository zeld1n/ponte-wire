package com.pontewire.worker.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public CommonErrorHandler kafkaErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer =
                new DeadLetterPublishingRecoverer(
                        kafkaTemplate,
                        (record, ex) -> new TopicPartition(record.topic() + ".dlq", record.partition())
                );

        // 2 retries after the first attempt with 5 seconds delay
        DefaultErrorHandler errorHandler =
                new DefaultErrorHandler(recoverer, new FixedBackOff(5000L, 2L));


        errorHandler.addNotRetryableExceptions(
                JsonProcessingException.class,
                IllegalArgumentException.class
        );

        return errorHandler;
    }

    @Bean
    public NewTopic incomingTopic() {
        return new NewTopic("pw.incoming", 3, (short) 1);
    }

    @Bean
    public NewTopic incomingDlqTopic() {
        return new NewTopic("pw.incoming.dlq", 3, (short) 1);
    }
}