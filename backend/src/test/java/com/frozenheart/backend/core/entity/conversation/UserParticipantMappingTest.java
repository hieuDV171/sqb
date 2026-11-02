package com.frozenheart.backend.core.entity.conversation;

import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserParticipantMappingTest {

    @Test
    void lastReadMessageAllowsMultipleParticipantsToReferenceTheSameMessage() throws Exception {
        Field field = UserParticipant.class.getDeclaredField("lastReadMessage");
        ManyToOne relation = field.getAnnotation(ManyToOne.class);

        assertNotNull(relation);
        assertEquals(FetchType.LAZY, relation.fetch());
    }
}
