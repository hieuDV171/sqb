package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.UpdateSubmissionSessionRequest;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SessionProposalValidationTest {
    @Test
    void requiresThreeQuestionsWhenCreatingOrReplacingTheQuestionList() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            var proposed = ProposeSessionRequest.QuestionProposeDto.builder().build();
            var edited = UpdateSubmissionSessionRequest.QuestionUpdateDto.builder().build();

            assertFalse(validator.validateProperty(
                    ProposeSessionRequest.builder().questions(List.of(proposed, proposed)).build(), "questions").isEmpty());
            assertTrue(validator.validateProperty(
                    ProposeSessionRequest.builder().questions(List.of(proposed, proposed, proposed)).build(), "questions").isEmpty());
            assertFalse(validator.validateProperty(
                    UpdateSubmissionSessionRequest.builder().questions(List.of(edited, edited)).build(), "questions").isEmpty());
            assertTrue(validator.validateProperty(UpdateSubmissionSessionRequest.builder().build(), "questions").isEmpty());
        }
    }
}
