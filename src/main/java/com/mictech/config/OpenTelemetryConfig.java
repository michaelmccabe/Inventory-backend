package com.mictech.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.opentelemetry.exporter.otlp.trace.OtlpGrpcSpanExporter;
import io.opentelemetry.sdk.trace.export.SpanExporter;

/**
 * As of SpringBoot 3.0.2 the inclusion of io.micrometer:micrometer-tracing-bridge-otel and
 * io.opentelemetry:opentelemetry-exporter-otlp is not sufficient to bootstrap the SpanExporter. Adding
 * io.opentelemetry:opentelemetry-sdk-extension-autoconfigure also does not help. Hence this solution which will
 * probably be redundant one day.
 */
@Configuration
public class OpenTelemetryConfig {

    @Value("${otel.exporter.otlp.traces.endpoint:http://localhost:4317}")
    private String tracesEndpoint;

    @Bean
    public SpanExporter spanExporter() {
        return OtlpGrpcSpanExporter.builder().setEndpoint(tracesEndpoint).build();
    }

}