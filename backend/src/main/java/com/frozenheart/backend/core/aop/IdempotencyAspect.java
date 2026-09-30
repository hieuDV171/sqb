package com.frozenheart.backend.core.aop;

import com.frozenheart.backend.core.annotation.Idempotent;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.MetricService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.Arrays;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class IdempotencyAspect {

    private final RedisTemplate<String, String> redisTemplate;
    private final JsonMapper jsonMapper;
    private final MetricService metricService;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CachedIdempotentResponse {
        private String status;
        private Integer statusCode;
        private Object body;
    }

    @Around("@annotation(idempotent)")
    public Object handleIdempotency(ProceedingJoinPoint joinPoint, Idempotent idempotent) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return joinPoint.proceed();
        }

        HttpServletRequest request = attributes.getRequest();
        String idempotencyKey = request.getHeader("X-Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            idempotencyKey = request.getHeader("Idempotency-Key");
        }

        Long userId = null;
        try {
            JwtPayload payload = JwtPayload.getCurrentUserPayload();
            if (payload != null) {
                userId = payload.getUserId();
            }
        } catch (Exception ignored) {
        }

        String userScope = userId != null ? "user:" + userId : "ip:" + request.getRemoteAddr();
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            // Fallback composite key if client did not explicitly provide one
            idempotencyKey = request.getMethod() + ":" + request.getRequestURI() + ":" + Arrays.deepToString(joinPoint.getArgs());
        }

        String redisKey = "idempotency:" + idempotent.keyPrefix() + ":" + userScope + ":" + idempotencyKey;

        // Phase 1: Lock TTL (PROCESSING) - chống Zombie Lock nếu server bị sự cố/kill giữa chừng
        Duration lockTtl = Duration.ofSeconds(idempotent.lockExpireSeconds());
        CachedIdempotentResponse processingState = CachedIdempotentResponse.builder()
                .status("PROCESSING")
                .build();
        String processingJson = jsonMapper.writeValueAsString(processingState);

        Boolean success = redisTemplate.opsForValue().setIfAbsent(redisKey, processingJson, lockTtl);
        if (Boolean.FALSE.equals(success)) {
            String currentVal = redisTemplate.opsForValue().get(redisKey);
            if (currentVal != null) {
                try {
                    CachedIdempotentResponse cached = jsonMapper.readValue(currentVal, CachedIdempotentResponse.class);
                    if ("COMPLETED".equals(cached.getStatus())) {
                        log.info("[IdempotencyAspect] Hit cached response for key: {}", redisKey);
                        metricService.incrementCounter("sqb.idempotency.blocked", "result", "cached", "key_prefix", idempotent.keyPrefix());
                        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
                        Class<?> returnType = signature.getReturnType();

                        int statusCode = cached.getStatusCode() != null ? cached.getStatusCode() : 200;
                        if (ResponseEntity.class.isAssignableFrom(returnType)) {
                            return ResponseEntity.status(statusCode)
                                    .header("X-Idempotency-Status", "CACHED")
                                    .body(cached.getBody());
                        } else {
                            return cached.getBody();
                        }
                    }
                } catch (Exception ex) {
                    log.warn("[IdempotencyAspect] Không thể giải mã cached response từ Redis: {}", ex.getMessage());
                }
            }

            log.warn("[IdempotencyAspect] Duplicate request detected for key: {}, current status: PROCESSING", redisKey);
            metricService.incrementCounter("sqb.idempotency.blocked", "result", "processing", "key_prefix", idempotent.keyPrefix());
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED,
                    "Yêu cầu này đang được xử lý, vui lòng không thao tác lại liên tục.");
        }

        try {
            Object result = joinPoint.proceed();

            // Phase 2: Cache TTL (COMPLETED) - Lưu kết quả thành công để trả lại cho các request trùng lặp
            try {
                Duration cacheTtl = Duration.ofSeconds(idempotent.expireSeconds());
                int statusCode = 200;
                Object body = result;
                if (result instanceof ResponseEntity<?> responseEntity) {
                    statusCode = responseEntity.getStatusCode().value();
                    body = responseEntity.getBody();
                }

                CachedIdempotentResponse completedState = CachedIdempotentResponse.builder()
                        .status("COMPLETED")
                        .statusCode(statusCode)
                        .body(body)
                        .build();

                String completedJson = jsonMapper.writeValueAsString(completedState);
                redisTemplate.opsForValue().set(redisKey, completedJson, cacheTtl);
            } catch (Exception e) {
                log.error("[IdempotencyAspect] Lỗi khi lưu cached response vào Redis: {}", e.getMessage(), e);
            }

            return result;
        } catch (Throwable ex) {
            // Nếu xử lý lỗi nghiệp vụ hoặc ngoại lệ, giải phóng key để client có thể retry hợp lệ
            redisTemplate.delete(redisKey);
            throw ex;
        }
    }
}
