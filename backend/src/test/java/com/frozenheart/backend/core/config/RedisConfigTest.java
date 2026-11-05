package com.frozenheart.backend.core.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;

class RedisConfigTest {

    @Test
    void leaderboardTemplateUsesStringSerializersForLuaArguments() {
        RedisConnectionFactory connectionFactory = mock(RedisConnectionFactory.class);

        StringRedisTemplate template = new RedisConfig().stringRedisTemplate(connectionFactory);

        assertSame(connectionFactory, template.getConnectionFactory());
        assertInstanceOf(StringRedisSerializer.class, template.getKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getValueSerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getHashKeySerializer());
        assertInstanceOf(StringRedisSerializer.class, template.getHashValueSerializer());
    }
}
