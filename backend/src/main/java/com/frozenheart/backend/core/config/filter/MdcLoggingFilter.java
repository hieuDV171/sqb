package com.frozenheart.backend.core.config.filter;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * Filter thiết lập Correlation ID (Trace ID) và ngữ cảnh chẩn đoán MDC (Mapped Diagnostic Context)
 * cho mọi HTTP request đi vào hệ thống. Đồng bộ chuẩn W3C Trace ID với Micrometer Tracing.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 5)
@RequiredArgsConstructor
public class MdcLoggingFilter extends OncePerRequestFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    public static final String REQUEST_ID_HEADER = "X-Request-ID";
    public static final String MDC_KEY_TRACE_ID = "traceId";
    public static final String MDC_KEY_CLIENT_IP = "clientIp";
    public static final String MDC_KEY_URI = "uri";
    public static final String MDC_KEY_METHOD = "method";

    private final ObjectProvider<Tracer> tracerProvider;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = resolveTraceId(request);

        // Lưu trữ các metadata chẩn đoán vào MDC
        MDC.put(MDC_KEY_TRACE_ID, traceId);
        MDC.put(MDC_KEY_CLIENT_IP, getClientIp(request));
        MDC.put(MDC_KEY_URI, request.getRequestURI());
        MDC.put(MDC_KEY_METHOD, request.getMethod());

        // Đính kèm Trace ID vào Response Header để client/frontend tiện đối soát khi gặp sự cố
        response.setHeader(CORRELATION_ID_HEADER, traceId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // Luôn dọn dẹp MDC để ngăn ngừa rò rỉ dữ liệu giữa các luồng Tomcat được tái sử dụng
            MDC.clear();
        }
    }

    private String resolveTraceId(HttpServletRequest request) {
        Tracer tracer = tracerProvider.getIfAvailable();
        if (tracer != null) {
            Span span = tracer.currentSpan();
            if (span != null) {
                TraceContext context = span.context();
                String otelTraceId = context.traceId();
                if (!otelTraceId.isBlank()) {
                    return otelTraceId;
                }
            }
        }
        return extractOrGenerateTraceId(request);
    }

    private String extractOrGenerateTraceId(HttpServletRequest request) {
        String traceId = request.getHeader(CORRELATION_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = request.getHeader(REQUEST_ID_HEADER);
        }
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }
        return traceId;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
