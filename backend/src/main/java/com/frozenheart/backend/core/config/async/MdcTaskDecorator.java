package com.frozenheart.backend.core.config.async;

import org.jspecify.annotations.NonNull;
import org.slf4j.MDC;
import org.springframework.core.task.TaskDecorator;

import java.util.Map;

/**
 * TaskDecorator sao chép toàn bộ MDC context từ luồng gọi chính (calling thread)
 * sang luồng thực thi tác vụ bất đồng bộ (worker thread trong ThreadPoolTaskExecutor).
 */
public class MdcTaskDecorator implements TaskDecorator {

    @Override
    public @NonNull Runnable decorate(@NonNull Runnable runnable) {
        Map<String, String> contextMap = MDC.getCopyOfContextMap();

        return () -> {
            try {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }
                runnable.run();
            } finally {
                MDC.clear();
            }
        };
    }
}
