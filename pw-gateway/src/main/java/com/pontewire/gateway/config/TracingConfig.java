package com.pontewire.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import zipkin2.reporter.AsyncReporter;
import zipkin2.reporter.brave.ZipkinSpanHandler;
import zipkin2.reporter.okhttp3.OkHttpSender;

@Configuration
public class TracingConfig {

    @Bean
    public ZipkinSpanHandler zipkinSpanHandler() {
        var sender = OkHttpSender.create("http://localhost:9411/api/v2/spans");
        var reporter = AsyncReporter.create(sender);
        return (ZipkinSpanHandler) ZipkinSpanHandler.create(reporter);
    }
}