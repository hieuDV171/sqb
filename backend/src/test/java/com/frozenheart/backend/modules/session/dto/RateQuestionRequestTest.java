package com.frozenheart.backend.modules.session.dto;

import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RateQuestionRequestTest {
    private final JsonMapper mapper = JsonMapper.builder()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    @Test
    void acceptsOneStarAndRejectsAnOldReportPayload() {
        var rating = mapper.readValue("""
                {"rating":0,"comment":"Nhận xét"}
                """, RateQuestionRequest.class);

        assertEquals(0, rating.rating());
        assertThrows(JacksonException.class, () -> mapper.readValue("""
                {"rating":0,"is_error":true,"comment":"Thiếu dữ kiện"}
                """, RateQuestionRequest.class));
    }
}
