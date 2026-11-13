package com.frozenheart.backend.modules.ai.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.questioneditlog.EditActorType;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLog;
import com.frozenheart.backend.core.entity.questioneditlog.QuestionEditLogStatus;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.ai.client.LocalAiClient;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.service.impl.AiIntegrationServiceImpl;
import com.frozenheart.backend.modules.session.repository.QuestionEditLogRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiRefinementReviewTest {
    private final LocalAiClient ai = mock(LocalAiClient.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionEditLogRepository logs = mock(QuestionEditLogRepository.class);
    private final AiIntegrationServiceImpl service = new AiIntegrationServiceImpl(ai,
            mock(BtpropHallucinationCheckerService.class), questions, mock(SessionRepository.class), logs,
            mock(UserRepository.class), JsonMapper.builder().build(), mock(ApplicationEventPublisher.class));

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void refineDoesNotCallAiForLecturerOutsideTheClass() {
        authenticate(2L);
        when(questions.findByIdFetchSessionAndCourseClass(7L)).thenReturn(Optional.of(question()));
        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> service.refineQuestion(7L, new AiRefineRequest("Làm rõ câu hỏi", null), EditActorType.LECTURER))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.REVIEW_PERMISSION_DENIED));
        verifyNoInteractions(ai, logs);
    }

    @Test
    void requiresAcknowledgementThenRejectsRepeatedApplication() {
        authenticate(1L);
        var question = question();
        var log = QuestionEditLog.builder().id(5L).question(question).status(QuestionEditLogStatus.PROPOSED)
                .afterState(Map.of("content", "Câu mới", "explanation", "Giải thích", "options", List.of(
                        Map.of("key", "A", "text", "Đúng", "is_correct", true),
                        Map.of("key", "B", "text", "Sai", "is_correct", false))))
                .hallucinationAudit(Map.of("violations", List.of(Map.of("type", "UNVERIFIED")))).build();
        when(logs.findByIdFetchQuestion(5L)).thenReturn(Optional.of(log));
        when(questions.save(question)).thenReturn(question);

        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> service.applyAiRefinement(7L, new AiApplyRequest(5L, true, false)))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.HALLUCINATION_DETECTED));
        assertThat(question.getContent()).isEqualTo("Câu gốc");
        service.applyAiRefinement(7L, new AiApplyRequest(5L, true, true));
        assertThat(question.getContent()).isEqualTo("Câu mới");
        assertThat(log.getStatus()).isEqualTo(QuestionEditLogStatus.APPLIED);
        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> service.applyAiRefinement(7L, new AiApplyRequest(5L, true, true)))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.ACTION_ALREADY_PERFORMED));
    }

    private Question question() {
        var courseClass = CourseClass.builder().lecturer(User.builder().id(1L).build()).build();
        var session = Session.builder().courseClass(courseClass).status(SessionStatus.REVIEWING).build();
        return Question.builder().id(7L).session(session).content("Câu gốc").build();
    }

    private void authenticate(long userId) {
        var auth = new UsernamePasswordAuthenticationToken("reviewer", null);
        auth.setDetails(JwtPayload.builder().userId(userId).role("LECTURER").build());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
