package com.pontewire.gateway;

import com.pontewire.gateway.config.RoutingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import reactor.core.publisher.Hooks;

@SpringBootApplication
@EnableConfigurationProperties(RoutingProperties.class)
public class PonteWireGatewayApplication {

	public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(PonteWireGatewayApplication.class, args);
	}

}
