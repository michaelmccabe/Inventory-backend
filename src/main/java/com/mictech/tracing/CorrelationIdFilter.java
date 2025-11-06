package com.mictech.tracing;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class CorrelationIdFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String TRACE_ID_HEADER = "Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {
        String correlationId = request.getHeader(CORRELATION_ID_HEADER);
        Context currentContext = Context.current();

        // If a correlation ID is provided, add it to the baggage
        if (correlationId != null && !correlationId.isEmpty()) {
            Baggage baggage = Baggage.builder().put("correlation.id", correlationId).build();
            currentContext = currentContext.with(baggage);
        }

        // Try-with-resources to ensure the context is managed correctly
        try (Scope scope = currentContext.makeCurrent()) {
            // Add the current trace ID to the response header
            String traceId = Span.current().getSpanContext().getTraceId();
            if (Span.current().getSpanContext().isValid()) {
                response.addHeader(TRACE_ID_HEADER, traceId);
            }

            // Continue the filter chain
            filterChain.doFilter(request, response);
        }
    }
}
