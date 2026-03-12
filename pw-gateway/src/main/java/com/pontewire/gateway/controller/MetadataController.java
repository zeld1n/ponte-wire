package com.pontewire.gateway.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.lang.management.ManagementFactory;
import java.time.Duration;

@RestController
public class MetadataController {


    public record SystemMetadata(
            String status,
            String version,
            String uptime,

            long freeMemoryMb,
            long totalMemoryMb,
            long maxMemoryMb,
            int processors,
            String jvmVersion
    ) {}

    @GetMapping("/api/v1/metadata")
    public Mono<SystemMetadata> getMetadata() {
        return Mono.fromCallable(() -> {
            long uptimeMillis = ManagementFactory.getRuntimeMXBean().getUptime();
            Duration duration = Duration.ofMillis(uptimeMillis);
            String formattedUptime = String.format("%dd %02dh %02dm %02ds",
                    duration.toDays(),
                    duration.toHoursPart(),
                    duration.toMinutesPart(),
                    duration.toSecondsPart());
            Runtime runtime = Runtime.getRuntime();
            long mb = 1024 * 1024;

            return new SystemMetadata(
                    "OPERATIONAL",
                    "0.1.0-SNAPSHOT",
                    formattedUptime,
                    runtime.freeMemory() / mb,
                    runtime.totalMemory() / mb,
                    runtime.maxMemory() / mb,
                    runtime.availableProcessors(),
                    System.getProperty("java.version")
            );
        });
    }
}