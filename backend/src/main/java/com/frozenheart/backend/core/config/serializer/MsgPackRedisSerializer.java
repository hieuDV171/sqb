package com.frozenheart.backend.core.config.serializer;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.msgpack.jackson.dataformat.MessagePackFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

// Jackson 2
public class MsgPackRedisSerializer implements RedisSerializer<Object> {

    private final ObjectMapper objectMapper;

    public MsgPackRedisSerializer() {
        this.objectMapper = new ObjectMapper(new MessagePackFactory());

        this.objectMapper.activateDefaultTyping(
                objectMapper.getPolymorphicTypeValidator(),
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );

    }

    @Override
    @NullMarked
    public byte[] serialize(@Nullable Object value) throws SerializationException {
        if (value == null) {
            return new byte[0];
        }
        try {
            return objectMapper.writeValueAsBytes(value);
        } catch (Exception e) {
            throw new SerializationException("Lỗi tuần tự hóa Object sang MsgPack: " + e.getMessage(), e);
        }
    }

    @Override
    public @Nullable Object deserialize(byte @Nullable [] bytes) throws SerializationException {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            return objectMapper.readValue(bytes, Object.class);
        } catch (Exception e) {
            throw new SerializationException("Lỗi giải tuần tự hóa MsgPack sang Object: " + e.getMessage(), e);
        }
    }
}
