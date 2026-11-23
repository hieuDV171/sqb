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
import com.frozenheart.backend.modules.ai.controller.AiIntegrationController;
import com.frozenheart.backend.modules.ai.dto.AiApplyRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineRequest;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;
import com.frozenheart.backend.modules.ai.service.impl.AiIntegrationServiceImpl;
import com.frozenheart.backend.modules.session.repository.QuestionEditLogRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AiRefinementReviewTest {
    private final LocalAiClient ai = mock(LocalAiClient.class);
    private final QuestionRepository questions = mock(QuestionRepository.class);
    private final QuestionEditLogRepository logs = mock(QuestionEditLogRepository.class);
    private final BtpropHallucinationCheckerService checker = mock(BtpropHallucinationCheckerService.class);
    private final AiIntegrationServiceImpl service = new AiIntegrationServiceImpl(ai,
            checker, questions, mock(SessionRepository.class), logs,
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
                .isThrownBy(() -> service.refineQuestion(7L, new AiRefineRequest("Làm rõ câu hỏi", null, null), EditActorType.LECTURER))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.REVIEW_PERMISSION_DENIED));
        verifyNoInteractions(ai, logs);
    }

    @Test
    void unavailableGeneratorDoesNotAuditOrStorePlaceholder() {
        authenticate(1L);
        when(questions.findByIdFetchSessionAndCourseClass(7L)).thenReturn(Optional.of(question()));
        when(ai.generateRequiredCompletion(anyString(), anyString()))
                .thenThrow(new AppException(ResponseCode.AI_SERVICE_UNAVAILABLE));
        var controller = new AiIntegrationController(service, JsonMapper.builder().build());
        var response = new MockHttpServletResponse();

        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> controller.streamRefinement(7L, new AiRefineRequest("Làm rõ câu hỏi", null, null), response))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.AI_SERVICE_UNAVAILABLE));
        assertThat(response.isCommitted()).isFalse();
        verifyNoInteractions(checker, logs);
    }

    @Test
    void sidecarOnlyConfigurationIsNotUsedAsTheGenerator() {
        var client = new LocalAiClient("http://ai-sidecar:8000/", "text-embedding-3-small", "",
                "http://ai-sidecar:8000");

        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> client.generateRequiredCompletion("system", "prompt"))
                .satisfies(error -> {
                    assertThat(error.getResponseCode()).isEqualTo(ResponseCode.AI_SERVICE_UNAVAILABLE);
                    assertThat(error.getMessage()).contains("chưa được cấu hình");
                });
    }

    @Test
    void generatorSendsTheSharedBearerKeyAndLunaCompatibilitySetting() throws IOException {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        var authorization = new AtomicReference<String>();
        var payload = new AtomicReference<String>();
        server.createContext("/v1/chat/completions", exchange -> {
            try {
                authorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
                payload.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
                byte[] response = """
                        {"model":"gpt-6-luna","choices":[{"message":{"content":"Bản đề xuất"}}]}
                        """.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(200, response.length);
                exchange.getResponseBody().write(response);
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            var client = new LocalAiClient("http://127.0.0.1:" + server.getAddress().getPort(),
                    "gpt-6-luna", "shared-fixture-key", "http://ai-sidecar:8000");

            var response = client.generateRequiredCompletion("system", "prompt");

            var request = JsonMapper.builder().build().readTree(payload.get());
            assertThat(authorization.get()).isEqualTo("Bearer shared-fixture-key");
            assertThat(request.path("model").asString()).isEqualTo("gpt-6-luna");
            assertThat(request.path("reasoning_effort").asString()).isEqualTo("none");
            assertThat(response.model()).isEqualTo("gpt-6-luna");
        } finally {
            server.stop(0);
        }
    }

    @Test
    void followUpStreamsProposalBeforeAuditingAndSaving() {
        authenticate(1L);
        var question = question();
        when(questions.findByIdFetchSessionAndCourseClass(7L)).thenReturn(Optional.of(question));
        var draft = QuestionEditLog.builder().afterState(Map.of("content", "Bản trước", "options", List.of(
                Map.of("key", "A", "text", "Đúng", "is_correct", true),
                Map.of("key", "B", "text", "Sai", "is_correct", false)))).build();
        when(logs.findByIdAndQuestionIdAndActorIdAndStatus(5L, 7L, 1L, QuestionEditLogStatus.PROPOSED))
                .thenReturn(Optional.of(draft));
        when(ai.generateRequiredCompletion(anyString(), argThat(prompt ->
                prompt.contains("Câu gốc") && prompt.contains("Bản trước") && prompt.contains("Rút ngắn lời giải"))))
                .thenReturn(LocalAiClient.LocalAiResponse.builder().model("test")
                        .content("""
                                {"content":"Bản mới","options":[{"key":"A","text":"Đúng","is_correct":true},
                                {"key":"B","text":"Sai","is_correct":false}]}
                                """).build());
        var response = new MockHttpServletResponse();
        when(checker.audit(any(), any())).thenAnswer(invocation -> {
            assertThat(response.isCommitted()).isTrue();
            assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8))
                    .contains("\"type\":\"proposal\"", "Bản mới", "\"model\":\"test\"")
                    .doesNotContain("\"type\":\"complete\"", "\"editLogId\"");
            return AiRefineResponse.BtpropAuditDto.builder().confidenceScore(0.9).build();
        });
        when(logs.save(any())).thenReturn(QuestionEditLog.builder().id(6L).build());

        var controller = new AiIntegrationController(service, JsonMapper.builder().build());
        controller.streamRefinement(7L, new AiRefineRequest("Rút ngắn lời giải", null, 5L), response);

        assertThat(response.getContentType()).startsWith("text/event-stream");
        assertThat(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8))
                .contains("\"type\":\"complete\"", "\"editLogId\":6", "\"confidenceScore\":0.9");
        verify(checker).audit(eq(question), argThat(suggestion -> "Bản mới".equals(suggestion.content())));
    }

    @Test
    void cannotUseDraftOutsideTheReviewerScope() {
        authenticate(1L);
        when(questions.findByIdFetchSessionAndCourseClass(7L)).thenReturn(Optional.of(question()));

        assertThatExceptionOfType(AppException.class)
                .isThrownBy(() -> service.refineQuestion(7L, new AiRefineRequest("Rút ngắn lời giải", null, 5L), EditActorType.LECTURER))
                .satisfies(error -> assertThat(error.getResponseCode()).isEqualTo(ResponseCode.INVALID_PARAMETER_VALUE));
        verify(logs).findByIdAndQuestionIdAndActorIdAndStatus(5L, 7L, 1L, QuestionEditLogStatus.PROPOSED);
        verifyNoInteractions(ai, checker);
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
