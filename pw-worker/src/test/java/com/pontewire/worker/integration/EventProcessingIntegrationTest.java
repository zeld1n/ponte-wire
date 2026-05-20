package com.pontewire.worker.integration;

import com.pontewire.worker.repository.EventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.config.TopicBuilder;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class EventProcessingIntegrationTest {

    @Container
    static final KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.6.1")
    );

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(
            DockerImageName.parse("postgres:16-alpine")
    )
            .withDatabaseName("pontewire_db")
            .withUsername("admin")
            .withPassword("admin");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.r2dbc.url", () ->
                "r2dbc:postgresql://" + postgres.getHost() + ":" +
                        postgres.getMappedPort(5432) + "/pontewire_db"
        );
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    KafkaTemplate<String, String> kafkaTemplate;

    @Autowired
    EventRepository eventRepository;

    @Autowired
    DatabaseClient databaseClient;

    @Autowired
    KafkaAdmin kafkaAdmin;

    @BeforeEach
    void setup() throws InterruptedException {
        databaseClient.sql("""
            CREATE TABLE IF NOT EXISTS processed_events (
                id          BIGSERIAL PRIMARY KEY,
                source      VARCHAR(255),
                payload     TEXT,
                received_at TIMESTAMP
            )
            """)
                .fetch()
                .rowsUpdated()
                .block();

    }
    @Test
    void whenMessageSentToKafka_thenPersistedToDatabase() throws Exception {
        String payload = """
            {
              "source": "stripe",
              "data": {"event": "payment.succeeded", "amount": 100},
              "timestamp": "2026-05-20T21:00:00Z"
            }
            """;

        kafkaTemplate.send("pw.incoming", UUID.randomUUID().toString(), payload)
                .get(5, TimeUnit.SECONDS);

        Mono<Long> countQuery = eventRepository.count()
                .filter(count -> count > 0)
                .repeatWhenEmpty(flux -> flux.delayElements(Duration.ofSeconds(1)).take(15));

        StepVerifier.create(countQuery)
                .assertNext(count -> assertThat(count).isGreaterThan(0))
                .verifyComplete();
    }
}