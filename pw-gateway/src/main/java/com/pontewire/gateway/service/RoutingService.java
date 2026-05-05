package com.pontewire.gateway.service;

import com.pontewire.gateway.config.RoutingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoutingService {

    private final RoutingProperties routingProperties;

    public String resolveTopic(String source) {
        String sourceLower = source.toLowerCase();

        String topic = routingProperties.getRoutes().get(sourceLower);

        if (topic == null) {
            topic = routingProperties.getDefaultTopic();
        }

        log.info("Source: {} → Topic: {}", source, topic);
        return topic;
    }
}