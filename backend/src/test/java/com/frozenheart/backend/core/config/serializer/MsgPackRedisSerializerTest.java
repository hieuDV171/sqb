package com.frozenheart.backend.core.config.serializer;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MsgPackRedisSerializerTest {

    @Test
    void readsExistingCacheAfterTheJacksonSecurityUpgrade() {
        // Written with Jackson 2.21.4 / MessagePack 0.9.12 and the production default typing.
        // Keep these bytes fixed so a dependency upgrade also checks existing Redis values.
        byte[] existingValue = Base64.getDecoder().decode(
                "hKZAY2xhc3O3amF2YS51dGlsLkxpbmtlZEhhc2hNYXCiaWSSrmphdmEubGFuZy5Mb25nB6V0aXRsZbRDw6J1IGjhu49pIMO0biB04bqtcKdhbnN3ZXJzkrNqYXZhLnV0aWwuQXJyYXlMaXN0kqvEkMOhcCDDoW4gQavEkMOhcCDDoW4gQg==");
        var expected = new LinkedHashMap<String, Object>();
        expected.put("id", 7L);
        expected.put("title", "Câu hỏi ôn tập");
        expected.put("answers", new ArrayList<>(List.of("Đáp án A", "Đáp án B")));

        var serializer = new MsgPackRedisSerializer();
        Object restored = serializer.deserialize(existingValue);

        assertEquals(expected, restored);
        // New writes must also be readable by a fresh serializer instance.
        assertEquals(expected, new MsgPackRedisSerializer().deserialize(serializer.serialize(restored)));
    }
}
