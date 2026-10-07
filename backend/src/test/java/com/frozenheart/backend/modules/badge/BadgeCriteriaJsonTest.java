package com.frozenheart.backend.modules.badge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frozenheart.backend.core.entity.badge.BadgeCriteria;
import com.frozenheart.backend.core.entity.badge.criteria.StreakCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BadgeCriteriaJsonTest {

    @Test
    void testSerializationAndDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        StreakCriteria streak = StreakCriteria.builder()
                .streakDays(7)
                .build();

        String json = mapper.writeValueAsString(streak);
        System.out.println("Serialized JSON: " + json);

        // Try deserializing as BadgeCriteria
        BadgeCriteria deserialized = mapper.readValue(json, BadgeCriteria.class);
        assertNotNull(deserialized);
        assertTrue(deserialized instanceof StreakCriteria);
        assertEquals(7, ((StreakCriteria) deserialized).getStreakDays());
    }

    @Test
    void testSnakeCaseAndCamelCaseDeserialization() throws Exception {
        ObjectMapper mapper = new ObjectMapper();

        // What if DB has snake_case vs camelCase or missing properties?
        String json1 = "{\"type\":\"STREAK\",\"streakDays\":7}";
        BadgeCriteria c1 = mapper.readValue(json1, BadgeCriteria.class);
        assertEquals(7, ((StreakCriteria) c1).getStreakDays());

        String json2 = "{\"type\":\"STREAK\",\"streak_days\":7}";
        try {
            BadgeCriteria c2 = mapper.readValue(json2, BadgeCriteria.class);
            System.out.println("Parsed json2: " + ((StreakCriteria) c2).getStreakDays());
        } catch (Exception e) {
            System.out.println("Failed json2: " + e.getMessage());
        }

        // What if JSON has string escaped in DB: "\"{\\\"type\\\":\\\"STREAK\\\",\\\"streakDays\\\":7}\""
        String jsonEscaped = "\"{\\\"type\\\":\\\"STREAK\\\",\\\"streakDays\\\":7}\"";
        try {
            BadgeCriteria c3 = mapper.readValue(jsonEscaped, BadgeCriteria.class);
            System.out.println("Parsed jsonEscaped: " + c3);
        } catch (Exception e) {
            System.out.println("Failed jsonEscaped: " + e.getMessage());
        }
    }
}
