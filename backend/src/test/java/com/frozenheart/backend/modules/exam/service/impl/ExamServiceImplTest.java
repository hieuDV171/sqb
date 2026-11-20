package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.ExamCourseClass;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.dto.GenerateExamRequest;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.exam.repository.ExamCourseClassRepository;
import com.frozenheart.backend.modules.exam.repository.ExamQuestionRepository;
import com.frozenheart.backend.modules.exam.repository.ExamRepository;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    private static final long OWNER_ID = 7L;
    private static final long EXAM_ID = 19L;

    @Mock private ExamRepository examRepository;
    @Mock private DocumentExportService documentExportService;
    @Mock private QuestionRepository questionRepository;
    @Mock private SubjectRepository subjectRepository;
    @Mock private UserRepository userRepository;
    @Mock private CourseClassRepository courseClassRepository;
    @Mock private ExamCourseClassRepository examCourseClassRepository;
    @Mock private ExamQuestionRepository examQuestionRepository;
    @Captor private ArgumentCaptor<Iterable<ExamCourseClass>> classLinks;
    @Spy private JsonMapper jsonMapper = JsonMapper.builder().build();
    @InjectMocks private ExamServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void studentCannotReadOrExportAnExam() {
        authenticate(OWNER_ID, UserRole.STUDENT);

        assertAccessDenied();
        verifyNoInteractions(examRepository, documentExportService);
    }

    @Test
    void anotherLecturerCannotReadOrExportAnExam() {
        authenticate(8L, UserRole.LECTURER);
        when(examRepository.findWithDetailsById(EXAM_ID)).thenReturn(Optional.of(ownedExam()));

        assertAccessDenied();
        verifyNoInteractions(documentExportService);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"LECTURER", "ADMIN"})
    void ownerAndAdminCanReadAndExportAnExam(UserRole role) {
        authenticate(role == UserRole.LECTURER ? OWNER_ID : 8L, role);
        Exam exam = ownedExam();
        when(examRepository.findWithDetailsById(EXAM_ID)).thenReturn(Optional.of(exam));
        var exported = ExportResponse.builder().downloadUrl("https://example.test/exam.pdf").build();
        when(documentExportService.exportExam(exam, "pdf", true)).thenReturn(exported);

        assertEquals(EXAM_ID, service.getExamDetail(EXAM_ID).getExamId());
        assertSame(exported, service.exportExam(EXAM_ID, "pdf", true));
    }

    @Test
    void savesBothMappedAssociationsWhenGeneratingAnExamForAClass() {
        authenticate(OWNER_ID, UserRole.LECTURER);
        var subject = Subject.builder().id(12L).name("Công nghệ Web").build();
        var courseClass = CourseClass.builder().id(31L).subject(subject).build();
        var lecturer = User.builder().id(OWNER_ID).build();
        var question = Question.builder().id(42L).status(QuestionStatus.APPROVED)
                .difficulty(QuestionDifficulty.UNCLASSIFIED).options(List.of()).build();
        when(subjectRepository.findById(subject.getId())).thenReturn(Optional.of(subject));
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(lecturer);
        when(questionRepository.findApprovedBySubjectId(subject.getId())).thenReturn(List.of(question));
        when(courseClassRepository.findAllByIds(List.of(courseClass.getId()))).thenReturn(List.of(courseClass));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(EXAM_ID);
            return saved;
        });

        var result = service.generateExam(GenerateExamRequest.builder().subjectId(subject.getId())
                .classIds(List.of(courseClass.getId())).questionCount(1)
                .difficultyDistribution(GenerateExamRequest.DifficultyDistribution.builder().build()).build());

        verify(examCourseClassRepository).saveAll(classLinks.capture());
        var link = classLinks.getValue().iterator().next();
        assertEquals(EXAM_ID, link.getId().getExamId());
        assertEquals(courseClass.getId(), link.getId().getCourseClassId());
        assertEquals(EXAM_ID, link.getExam().getId());
        assertSame(courseClass, link.getCourseClass());
        assertEquals(1, result.getQuestionCount());
        assertEquals(1, result.getStatistic().getUnclassifiedCount());
        assertEquals(question.getId(), result.getQuestions().getFirst().getQuestionId());
    }

    private void assertAccessDenied() {
        assertEquals(ResponseCode.ACCESS_DENIED,
                assertThrows(AppException.class, () -> service.getExamDetail(EXAM_ID)).getResponseCode());
        assertEquals(ResponseCode.ACCESS_DENIED,
                assertThrows(AppException.class, () -> service.exportExam(EXAM_ID, "pdf", true)).getResponseCode());
    }

    private Exam ownedExam() {
        return Exam.builder().id(EXAM_ID).lecturer(User.builder().id(OWNER_ID).build()).questionCount(0).build();
    }

    private void authenticate(long userId, UserRole role) {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(userId).role(role.name()).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
