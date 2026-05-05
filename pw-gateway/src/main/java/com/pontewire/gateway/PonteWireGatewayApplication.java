package com.pontewire.gateway;

import com.pontewire.gateway.config.RoutingProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(RoutingProperties.class)
public class PonteWireGatewayApplication {

	public static void main(String[] args) {
		SpringApplication.run(PonteWireGatewayApplication.class, args);
	}

}
