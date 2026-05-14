package com.pontewire.worker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@SpringBootApplication
public class PwWorkerApplication {

	public static void main(String[] args) {
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(PwWorkerApplication.class, args);
	}

}
