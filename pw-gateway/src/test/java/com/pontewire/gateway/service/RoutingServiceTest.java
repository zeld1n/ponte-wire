package com.pontewire.gateway.service;

import com.pontewire.gateway.config.RoutingProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RoutingServiceTest {

    private RoutingService routingService;

    @BeforeEach
    void setUp() {
        RoutingProperties properties = new RoutingProperties();
        properties.setDefaultTopic("pw.incoming");
        properties.setRoutes(Map.of(
                "stripe",  "pw.incoming.payments",
                "github",  "pw.incoming.devops",
                "shopify", "pw.incoming.commerce"
        ));

        routingService = new RoutingService(properties);
    }

    @Test
    void stripe_shouldRouteToPayments() {
        String topic = routingService.resolveTopic("stripe");
        assertThat(topic).isEqualTo("pw.incoming.payments");
    }

    @Test
    void github_shouldRouteToDevops() {
        String topic = routingService.resolveTopic("github");
        assertThat(topic).isEqualTo("pw.incoming.devops");
    }

    @Test
    void unknownSource_shouldRouteToDefaultTopic() {
        String topic = routingService.resolveTopic("unknown-service");
        assertThat(topic).isEqualTo("pw.incoming");
    }

    @Test
    void source_shouldBeCaseInsensitive() {
        String topic1 = routingService.resolveTopic("STRIPE");
        String topic2 = routingService.resolveTopic("stripe");
        assertThat(topic1).isEqualTo(topic2);
    }
}