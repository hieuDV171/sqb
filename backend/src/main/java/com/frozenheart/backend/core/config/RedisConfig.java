package com.frozenheart.backend.core.config;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.cache.interceptor.SimpleCacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import com.frozenheart.backend.core.config.serializer.MsgPackRedisSerializer;

@Configuration
@Slf4j
public class RedisConfig implements CachingConfigurer {

    @Bean
    public RedisTemplate<String, String> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, String> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        MsgPackRedisSerializer msgPackRedisSerializer = new MsgPackRedisSerializer();

        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(msgPackRedisSerializer);

        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(msgPackRedisSerializer);

        template.afterPropertiesSet();

        return template;
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new SimpleCacheErrorHandler() {
            @Override
            public void handleCacheGetError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("[CẢNH BÁO] Không thể kết nối tới Redis để LẤY cache. Tự động chuyển hướng xuống Database! Chi tiết: {}", exception.getMessage());
            }

            @Override
            public void handleCachePutError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key, Object value) {
                log.error("[CẢNH BÁO] Không thể LƯU vào Redis. Chi tiết: {}", exception.getMessage());
            }

            @Override
            public void handleCacheEvictError(@NonNull RuntimeException exception, @NonNull Cache cache, @NonNull Object key) {
                log.error("[CẢNH BÁO] Không thể XÓA cache trong Redis. Chi tiết: {}", exception.getMessage());
            }

            @Override
            public void handleCacheClearError(@NonNull RuntimeException exception, @NonNull Cache cache) {
                log.error("[CẢNH BÁO] Không thể XÓA TOÀN BỘ cache. Chi tiết: {}", exception.getMessage());
            }
        };
    }
}
