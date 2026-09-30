package com.frozenheart.backend.core.util;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Service tiện ích hỗ trợ ghi nhận các Business Metrics tùy chỉnh vào Prometheus / Micrometer.
 * Thiết kế Zero-Impact: Mọi thao tác ghi metrics đều được bảo vệ để không bao giờ làm gián đoạn luồng nghiệp vụ.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MetricService {

    private final MeterRegistry meterRegistry;

    /**
     * Tăng giá trị của một counter lên 1 kèm theo các nhãn (tags).
     */
    public void incrementCounter(String name, String... tags) {
        try {
            Counter.builder(name)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment();
        } catch (Exception e) {
            log.debug("[MetricService] Không thể ghi nhận counter '{}': {}", name, e.getMessage());
        }
    }

    /**
     * Tăng giá trị counter theo một lượng chỉ định.
     */
    public void incrementCounter(String name, double amount, String... tags) {
        try {
            Counter.builder(name)
                    .tags(tags)
                    .register(meterRegistry)
                    .increment(amount);
        } catch (Exception e) {
            log.debug("[MetricService] Không thể ghi nhận counter '{}' (+{}): {}", name, amount, e.getMessage());
        }
    }

    /**
     * Đo lường thời gian thực thi của một hành động (Runnable).
     */
    public void recordTime(String name, Runnable runnable, String... tags) {
        try {
            Timer timer = Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry);
            timer.record(runnable);
        } catch (Exception e) {
            log.debug("[MetricService] Lỗi khi đo thời gian runnable '{}': {}", name, e.getMessage());
            runnable.run();
        }
    }

    /**
     * Đo lường thời gian thực thi của một tác vụ trả về giá trị (Supplier).
     */
    public <T> T recordTime(String name, Supplier<T> supplier, String... tags) {
        try {
            Timer timer = Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry);
            return timer.record(supplier);
        } catch (Exception e) {
            log.debug("[MetricService] Lỗi khi đo thời gian supplier '{}': {}", name, e.getMessage());
            return supplier.get();
        }
    }

    /**
     * Ghi nhận trực tiếp khoảng thời gian thực thi đã tính trước (milliseconds).
     */
    public void recordDuration(String name, long durationMs, String... tags) {
        try {
            Timer timer = Timer.builder(name)
                    .tags(tags)
                    .register(meterRegistry);
            timer.record(durationMs, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.debug("[MetricService] Không thể ghi nhận duration '{}' ({}ms): {}", name, durationMs, e.getMessage());
        }
    }
}
