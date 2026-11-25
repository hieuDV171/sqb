package com.frozenheart.backend.core.entity.session;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class QuestionOptionTest {

    @Test
    void jsonCopyMatchesOriginalUntilAnAnswerChanges() {
        var option = new QuestionOption();
        option.setKey("A");
        option.setText("HTTP");
        option.setIsCorrect(true);
        option.setMediaUrl("/media/answer.png");
        option.setMediaId(42L);

        var mapper = JsonMapper.builder().build();
        var options = List.of(option);
        var listType = mapper.getTypeFactory().constructCollectionType(List.class, QuestionOption.class);
        List<QuestionOption> snapshot = mapper.readValue(mapper.writeValueAsBytes(options), listType);

        assertNotSame(option, snapshot.getFirst());
        assertEquals(options, snapshot);
        assertEquals(options.hashCode(), snapshot.hashCode());

        option.setIsCorrect(false);

        assertNotEquals(options, snapshot);
    }
}
