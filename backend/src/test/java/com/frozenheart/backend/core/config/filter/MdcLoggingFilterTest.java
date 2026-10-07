package com.frozenheart.backend.core.config.filter;

import com.frozenheart.backend.core.config.async.MdcTaskDecorator;

import io.micrometer.tracing.Tracer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.MDC;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class MdcLoggingFilterTest {

    private final ObjectProvider<Tracer> tracerProvider = 
            Mockito.mock(ObjectProvider.class);
    private final MdcLoggingFilter filter = new MdcLoggingFilter(tracerProvider);

    @Test
    @DisplayName("Nên tái sử dụng Correlation ID được truyền vào từ header X-Correlation-ID")
    void shouldReuseIncomingCorrelationId() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/posts");
        request.setMethod("GET");
        String customTraceId = "custom-trace-id-12345";
        request.addHeader(MdcLoggingFilter.CORRELATION_ID_HEADER, customTraceId);

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcTraceIdDuringExecution = new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcTraceIdDuringExecution.set(MDC.get(MdcLoggingFilter.MDC_KEY_TRACE_ID));
            }
        };

        filter.doFilter(request, response, filterChain);

        // Kiểm tra trong quá trình request thực thi
        assertEquals(customTraceId, mdcTraceIdDuringExecution.get());
        // Kiểm tra header phản hồi
        assertEquals(customTraceId, response.getHeader(MdcLoggingFilter.CORRELATION_ID_HEADER));
        // Kiểm tra MDC đã được dọn dẹp sau khi request kết thúc
        assertNull(MDC.get(MdcLoggingFilter.MDC_KEY_TRACE_ID));
    }

    @Test
    @DisplayName("Nên tự động sinh Correlation ID (UUID) mới khi request không có header")
    void shouldGenerateNewCorrelationIdWhenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/auth/login");
        request.setMethod("POST");

        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> mdcTraceIdDuringExecution = new AtomicReference<>();

        MockFilterChain filterChain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                mdcTraceIdDuringExecution.set(MDC.get(MdcLoggingFilter.MDC_KEY_TRACE_ID));
            }
        };

        filter.doFilter(request, response, filterChain);

        String generatedTraceId = mdcTraceIdDuringExecution.get();
        assertNotNull(generatedTraceId);
        assertFalse(generatedTraceId.isBlank());
        assertEquals(generatedTraceId, response.getHeader(MdcLoggingFilter.CORRELATION_ID_HEADER));
        // Dọn dẹp an toàn
        assertNull(MDC.get(MdcLoggingFilter.MDC_KEY_TRACE_ID));
    }

    @Test
    @DisplayName("TaskDecorator nên sao chép chính xác ngữ cảnh MDC sang luồng con và dọn dẹp sau khi chạy xong")
    void shouldPropagateMdcToAsyncThreads() throws Exception {
        MDC.put("traceId", "async-trace-999");
        MDC.put("userId", "101");

        MdcTaskDecorator decorator = new MdcTaskDecorator();
        AtomicReference<String> asyncTraceId = new AtomicReference<>();
        AtomicReference<String> asyncUserId = new AtomicReference<>();

        Runnable task = decorator.decorate(() -> {
            asyncTraceId.set(MDC.get("traceId"));
            asyncUserId.set(MDC.get("userId"));
        });

        // Chạy trên một luồng khác
        CompletableFuture<Void> future = CompletableFuture.runAsync(task);
        future.get();

        assertEquals("async-trace-999", asyncTraceId.get());
        assertEquals("101", asyncUserId.get());

        // Dọn dẹp luồng cha
        MDC.clear();
    }
}
