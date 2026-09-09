package com.frozenheart.backend.core.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Idempotent {
    String keyPrefix() default "idempotency";

    /**
     * Thời gian khóa khi đang xử lý (Phase 1: Lock TTL).
     * Mặc định 30 giây để chống Zombie Lock nếu server bị sự cố/kill giữa chừng.
     */
    long lockExpireSeconds() default 30;

    /**
     * Thời gian lưu kết quả sau khi xử lý thành công (Phase 2: Cache TTL).
     * Mặc định 120 giây để trả về cached response cho các request trùng lặp.
     */
    long expireSeconds() default 120;
}
