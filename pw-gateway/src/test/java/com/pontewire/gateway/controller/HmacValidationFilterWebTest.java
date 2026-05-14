package com.pontewire.gateway.controller;

import com.pontewire.gateway.component.HmacValidationFilter;
import com.pontewire.gateway.config.RoutingProperties;
import com.pontewire.gateway.service.RoutingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import(HmacValidationFilter.class)
public class HmacValidationFilterWebTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private RoutingService routingService;

    @MockBean
    private KafkaTemplate<String, String> kafkaTemplate;

    @MockBean
    private RoutingProperties routingProperties;

    @Test
    void missingSignatureReturns401() {
        webTestClient.post()
                .uri("/webhook/stripe")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}
