package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.session.dto.ProposeSessionRequest;
import com.frozenheart.backend.modules.session.dto.ProposeSessionResponse;
import com.frozenheart.backend.modules.session.repository.QuestionMediaRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.service.DuplicateDetectionService;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionServiceImplTest {

    @Mock
    private AnonymizerUtil anonymizerUtil;
    @Mock
    private UserRepository userRepository;
    @Mock
    private SubjectRepository subjectRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionMediaRepository questionMediaRepository;
    @Mock
    private DuplicateDetectionService duplicateDetectionService;
    @Mock
    private CounterMetricsService counterMetricsService;
    @Mock
    private UserCourseClassRepository userCourseClassRepository;

    @InjectMocks
    private SessionServiceImpl sessionService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Phải gán displayOrder cho mọi câu hỏi trước khi lưu")
    void proposeSessionShouldAssignDisplayOrderBeforeSavingQuestions() {
        Long userId = 7L;
        Long subjectId = 11L;
        setCurrentUser(userId);

        User proposer = User.builder().id(userId).role(UserRole.ADMIN).build();
        Subject subject = Subject.builder().id(subjectId).code("IT4788").name("Phát triển ứng dụng đa nền tảng").build();

        when(userRepository.findById(userId)).thenReturn(java.util.Optional.of(proposer));
        when(subjectRepository.findById(subjectId)).thenReturn(java.util.Optional.of(subject));
        when(anonymizerUtil.encodeUserId(userId)).thenReturn("ABC123");
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(101L);
            return session;
        });
        when(questionRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Question> questions = invocation.getArgument(0);
            assertEquals(List.of(1, 2), questions.stream().map(Question::getDisplayOrder).toList());
            for (int index = 0; index < questions.size(); index++) {
                questions.get(index).setId(201L + index);
            }
            return questions;
        });

        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên câu hỏi kiểm thử",
                "Kiểm tra thứ tự câu hỏi",
                null,
                subjectId,
                List.of(question("Câu hỏi thứ nhất"), question("Câu hỏi thứ hai")));

        ProposeSessionResponse response = sessionService.proposeSession(request);

        assertNotNull(response);
        assertEquals(101L, response.sessionId());
        assertEquals(2, response.questionCount());
        verify(questionRepository).saveAll(anyList());
        verify(counterMetricsService).incrementProposedQuestions(userId, 2);
        verify(duplicateDetectionService).asyncCheckDuplicates(101L);
    }

    @Test
    @DisplayName("Phải từ chối request null trước khi truy cập repository")
    void proposeSessionShouldRejectNullRequestBeforeRepositoryAccess() {
        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(null));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Dữ liệu đề xuất phiên không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("Phải từ chối danh sách câu hỏi null trước khi truy cập repository")
    void proposeSessionShouldRejectNullQuestionsBeforeRepositoryAccess() {
        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên thiếu danh sách câu hỏi",
                null,
                null,
                11L,
                null);

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(request));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Danh sách câu hỏi không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    @Test
    @DisplayName("Phải từ chối danh sách câu hỏi rỗng trước khi truy cập repository")
    void proposeSessionShouldRejectEmptyQuestionsBeforeRepositoryAccess() {
        ProposeSessionRequest request = new ProposeSessionRequest(
                "Phiên không có câu hỏi",
                null,
                null,
                11L,
                List.of());

        AppException exception = assertThrows(
                AppException.class,
                () -> sessionService.proposeSession(request));

        assertEquals(ResponseCode.MISSING_REQUIRED_PARAMETER, exception.getResponseCode());
        assertEquals("Danh sách câu hỏi không được để trống", exception.getMessage());
        verifyNoRepositoryInteractions();
    }

    private ProposeSessionRequest.QuestionProposeDto question(String content) {
        QuestionOption option = new QuestionOption();
        option.setKey("A");
        option.setText("Đáp án đúng");
        option.setIsCorrect(true);

        return new ProposeSessionRequest.QuestionProposeDto(
                content,
                null,
                List.of(option),
                null,
                false,
                3.0);
    }

    private void setCurrentUser(Long userId) {
        JwtPayload payload = JwtPayload.builder().userId(userId).role(UserRole.ADMIN.name()).build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private void verifyNoRepositoryInteractions() {
        verifyNoInteractions(
                userRepository,
                subjectRepository,
                sessionRepository,
                questionRepository,
                questionMediaRepository,
                userCourseClassRepository);
    }
}
