package com.pontewire.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "pontewire.routing")
public class RoutingProperties {

    private String defaultTopic = "pw.incoming";
    private Map<String, String> routes = new HashMap<>();
}