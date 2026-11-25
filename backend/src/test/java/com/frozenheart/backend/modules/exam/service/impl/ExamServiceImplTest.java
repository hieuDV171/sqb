package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionDifficulty;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.session.Topic;
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
import com.frozenheart.backend.modules.session.repository.TopicRepository;
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
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
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
    @Mock private TopicRepository topicRepository;
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
                .difficultyDistribution(GenerateExamRequest.DifficultyDistribution.builder().unclassified(1.0).build()).build());

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

    @Test
    void refusesToSaveWhenBothMatricesAreIndividuallyFeasibleButCannotBeMetTogether() {
        authenticate(OWNER_ID, UserRole.LECTURER);
        var subject = Subject.builder().id(12L).build();
        var first = Topic.builder().id(1L).name("A").build();
        var second = Topic.builder().id(2L).name("B").build();
        when(subjectRepository.findById(12L)).thenReturn(Optional.of(subject));
        when(topicRepository.findBySubjectIdOrderByNameAsc(12L)).thenReturn(List.of(first, second));
        when(questionRepository.findApprovedBySubjectId(12L)).thenReturn(List.of(
                bankQuestion(1L, first, QuestionDifficulty.EASY), bankQuestion(2L, first, QuestionDifficulty.EASY),
                bankQuestion(3L, second, QuestionDifficulty.HARD), bankQuestion(4L, second, QuestionDifficulty.HARD),
                bankQuestion(5L, second, QuestionDifficulty.HARD)));

        var request = GenerateExamRequest.builder().subjectId(12L).classIds(List.of(31L)).questionCount(4)
                .difficultyDistribution(GenerateExamRequest.DifficultyDistribution.builder().easy(0.25).hard(0.75).build())
                .topicWeights(Map.of("1", 0.5, "2", 0.5)).build();

        var error = assertThrows(AppException.class, () -> service.generateExam(request));
        assertEquals(ResponseCode.INVALID_PARAMETER_VALUE, error.getResponseCode());
        assertTrue(error.getMessage().contains("3/4"));
        verifyNoInteractions(examRepository, examCourseClassRepository, examQuestionRepository);
    }

    @Test
    void reallocatesSharedDifficultySlotsAndSnapshotsTheActualMatrix() {
        authenticate(OWNER_ID, UserRole.LECTURER);
        var subject = Subject.builder().id(12L).name("Công nghệ Web").build();
        var first = Topic.builder().id(1L).name("A").build();
        var second = Topic.builder().id(2L).name("B").build();
        var hard = bankQuestion(2L, first, QuestionDifficulty.HARD);
        var owner = User.builder().id(OWNER_ID).build();
        when(subjectRepository.findById(12L)).thenReturn(Optional.of(subject));
        when(userRepository.getReferenceById(OWNER_ID)).thenReturn(owner);
        when(topicRepository.findBySubjectIdOrderByNameAsc(12L)).thenReturn(List.of(first, second));
        when(questionRepository.findApprovedBySubjectId(12L)).thenReturn(List.of(
                bankQuestion(1L, first, QuestionDifficulty.EASY), hard, bankQuestion(3L, second, QuestionDifficulty.EASY)));
        when(courseClassRepository.findAllByIds(List.of(31L)))
                .thenReturn(List.of(CourseClass.builder().id(31L).subject(subject).build()));
        var savedExam = ArgumentCaptor.forClass(Exam.class);
        when(examRepository.save(savedExam.capture())).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(EXAM_ID);
            return saved;
        });

        var result = service.generateExam(GenerateExamRequest.builder().subjectId(12L).classIds(List.of(31L))
                .questionCount(2).topicWeights(Map.of("1", 0.5, "2", 0.5))
                .difficultyDistribution(GenerateExamRequest.DifficultyDistribution.builder().easy(0.5).hard(0.5).build())
                .build());

        assertEquals(List.of(2L, 3L), result.getQuestions().stream().map(q -> q.getQuestionId()).sorted().toList());
        assertEquals(1, result.getTopicMatrix().rows().getFirst().actualByDifficulty().getHardCount());

        first.setName("Đã đổi tên");
        hard.setDifficulty(QuestionDifficulty.MEDIUM);
        when(examRepository.findWithDetailsById(EXAM_ID)).thenReturn(Optional.of(savedExam.getValue()));
        var reopened = service.getExamDetail(EXAM_ID);
        assertEquals("A", reopened.getTopicMatrix().rows().getFirst().topicName());
        assertEquals(0.5, reopened.getDifficultyMatrix().requested().getHard());
        assertEquals(1, reopened.getStatistic().getHardCount());
    }

    @Test
    void countsUntaggedQuestionsByDifficultyWithoutIncludingArchivedTopicsInThatGroup() {
        authenticate(OWNER_ID, UserRole.LECTURER);
        var active = Topic.builder().id(1L).name("HTTP").build();
        var archived = Topic.builder().id(2L).name("Archive").archived(true).build();
        when(topicRepository.countTeachingAssignments(12L, OWNER_ID)).thenReturn(1L);
        when(subjectRepository.existsById(12L)).thenReturn(true);
        when(topicRepository.findBySubjectIdOrderByNameAsc(12L)).thenReturn(List.of(active, archived));
        var counts = List.of(
                bankCount(1L, QuestionDifficulty.EASY, 2), bankCount(2L, QuestionDifficulty.HARD, 3),
                bankCount(null, QuestionDifficulty.HARD, 4), bankCount(null, null, 5));
        when(questionRepository.countExamBankByTopicAndDifficulty(12L)).thenReturn(counts);

        var summary = service.getBankSummary(12L);
        var untagged = summary.topics().getLast();
        assertEquals(14, summary.totalQuestions());
        assertEquals(2, summary.topics().size());
        assertEquals(1L, summary.topics().getFirst().topicId());
        assertNull(untagged.topicId());
        assertEquals("Chưa được phân loại chủ đề", untagged.topicName());
        assertEquals(9, untagged.totalQuestions());
        assertEquals(4, untagged.statistic().getHardCount());
        assertEquals(5, untagged.statistic().getUnclassifiedCount());
    }

    @Test
    void appliesUntaggedWeightAndSnapshotsItWithTheOtherTopics() {
        authenticate(OWNER_ID, UserRole.LECTURER);
        var subject = Subject.builder().id(12L).build();
        var active = Topic.builder().id(1L).name("HTTP").build();
        when(subjectRepository.findById(12L)).thenReturn(Optional.of(subject));
        when(topicRepository.findBySubjectIdOrderByNameAsc(12L)).thenReturn(List.of(active));
        when(questionRepository.findApprovedBySubjectId(12L)).thenReturn(List.of(
                bankQuestion(1L, active, QuestionDifficulty.EASY), bankQuestion(2L, null, QuestionDifficulty.HARD)));
        when(examRepository.save(any(Exam.class))).thenAnswer(invocation -> {
            Exam saved = invocation.getArgument(0);
            saved.setId(EXAM_ID);
            return saved;
        });

        var result = service.generateExam(GenerateExamRequest.builder().subjectId(12L).title("Đề thử").classIds(List.of(31L))
                .questionCount(2).topicWeights(Map.of("1", 0.5, "untagged", 0.5))
                .difficultyDistribution(GenerateExamRequest.DifficultyDistribution.builder().easy(0.5).hard(0.5).build())
                .build());

        var untagged = result.getTopicMatrix().rows().getLast();
        assertEquals(List.of(1L, 2L), result.getQuestions().stream().map(q -> q.getQuestionId()).sorted().toList());
        assertNull(untagged.topicId());
        assertEquals(0.5, untagged.requestedWeight());
        assertEquals(1, untagged.requestedCount());
        assertEquals(1, untagged.actualByDifficulty().getHardCount());
    }

    @Test
    void doesNotUseQuestionsWithArchivedTopicsToFillUntaggedQuota() {
        var archived = Topic.builder().id(2L).name("Archive").archived(true).build();
        var bank = List.of(bankQuestion(1L, archived, QuestionDifficulty.HARD));

        var error = assertThrows(AppException.class,
                () -> ExamQuestionSelection.select(bank, 1, null, Map.of("untagged", 1.0), List.of(archived)));
        assertTrue(error.getMessage().contains("Chưa được phân loại chủ đề có 0/1 câu"));
    }

    private static QuestionRepository.ExamBankCount bankCount(Long topicId, QuestionDifficulty difficulty, long count) {
        var row = mock(QuestionRepository.ExamBankCount.class);
        when(row.getTopicId()).thenReturn(topicId);
        when(row.getDifficulty()).thenReturn(difficulty);
        when(row.getQuestionCount()).thenReturn(count);
        return row;
    }

    private static Question bankQuestion(Long id, Topic topic, QuestionDifficulty difficulty) {
        return Question.builder().id(id).status(QuestionStatus.APPROVED).topic(topic).difficulty(difficulty).build();
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
