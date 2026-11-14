package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.config.property.R2Properties;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocumentExportServiceImplTest {

    private static final long USER_ID = 7L;
    private static final long SUBJECT_ID = 12L;

    @Mock private MinioClient minioClient;
    @Mock private QuestionRepository questionRepository;
    @Mock private CourseClassRepository courseClassRepository;
    @Mock private MediaService mediaService;

    private R2Properties properties;
    private DocumentExportServiceImpl service;

    @BeforeEach
    void setUp() {
        properties = new R2Properties();
        properties.setBucketName("sqb-public");
        properties.setReportEvidenceBucket("sqb-secret");
        service = new DocumentExportServiceImpl(minioClient, properties, questionRepository, courseClassRepository, mediaService);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"LECTURER", "STUDENT"})
    void deniesExportBeforeReadingQuestionsOrSigningAUrl(UserRole role) {
        authenticate(role);

        AppException error = assertThrows(AppException.class, () -> service.exportQuestions(SUBJECT_ID, "pdf", true));

        assertEquals(ResponseCode.ACCESS_DENIED, error.getResponseCode());
        if (role == UserRole.LECTURER) {
            verify(courseClassRepository).existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(USER_ID, SUBJECT_ID);
        } else {
            verifyNoInteractions(courseClassRepository);
        }
        verifyNoInteractions(questionRepository, minioClient, mediaService);
    }

    @ParameterizedTest
    @EnumSource(value = UserRole.class, names = {"ADMIN", "LECTURER"})
    void exportsQuestionsOfAllStatusesForAnAuthorizedSubject(UserRole role) throws Exception {
        authenticate(role);
        if (role == UserRole.LECTURER) {
            when(courseClassRepository.existsByLecturerIdAndSubjectIdAndSemesterActiveTrue(USER_ID, SUBJECT_ID))
                    .thenReturn(true);
        }
        when(questionRepository.findAllBySubjectIdForExport(SUBJECT_ID)).thenReturn(List.of(
                Question.builder().content("Câu đã duyệt").status(QuestionStatus.APPROVED).build(),
                Question.builder().content("Câu chờ duyệt").status(QuestionStatus.PENDING).build(),
                Question.builder().content("Câu bị từ chối").status(QuestionStatus.REJECTED).build()));
        when(minioClient.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class)))
                .thenReturn("https://example.test/study-material.xlsx");

        var result = service.exportQuestions(SUBJECT_ID, "excel", true);

        assertEquals(3, result.getQuestionsCount());
        assertEquals("https://example.test/study-material.xlsx", result.getDownloadUrl());
        verifyNewPrivateExport("exports/questions/subject_12_ans_true.xlsx");
        if (role == UserRole.ADMIN) {
            verifyNoInteractions(courseClassRepository);
        }
    }

    @Test
    void uploadsAndSignsNewExamInPrivateBucket() throws Exception {
        service.exportExam(Exam.builder().id(19L).build(), "xlsx", false, "A4");

        verifyNewPrivateExport("exports/exams/exam_19_ans_false.xlsx");
    }

    @Test
    void signsCachedPdfInPrivateBucketWithoutUploading() throws Exception {
        var cached = mock(StatObjectResponse.class);
        when(minioClient.statObject(any(StatObjectArgs.class))).thenReturn(cached);

        service.exportExam(Exam.builder().id(19L).build(), "pdf", true, "A4");

        String key = "exports/exams/exam_19_ans_true.pdf";
        verifyPrivateLookupAndLink(key);
        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
        verifyNoInteractions(mediaService);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "sqb-public"})
    void refusesBothExportsWithoutADistinctPrivateBucket(String bucket) {
        properties.setReportEvidenceBucket(bucket);
        authenticate(UserRole.ADMIN);

        assertEquals(ResponseCode.FILE_UPLOAD_FAILED,
                assertThrows(AppException.class, () -> service.exportQuestions(SUBJECT_ID, "pdf", true)).getResponseCode());
        assertEquals(ResponseCode.FILE_UPLOAD_FAILED,
                assertThrows(AppException.class,
                        () -> service.exportExam(Exam.builder().id(19L).build(), "pdf", true, "A4")).getResponseCode());
        verifyNoInteractions(courseClassRepository, questionRepository, minioClient, mediaService);
    }

    private void verifyNewPrivateExport(String key) throws Exception {
        verifyPrivateLookupAndLink(key);
        verify(minioClient).putObject(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)));
        verify(mediaService).confirmMediaPermanent(List.of(key));
    }

    private void verifyPrivateLookupAndLink(String key) throws Exception {
        verify(minioClient).statObject(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)));
        verify(minioClient).getPresignedObjectUrl(argThat(args -> args.bucket().equals("sqb-secret") && args.object().equals(key)));
    }

    private void authenticate(UserRole role) {
        var authentication = new UsernamePasswordAuthenticationToken("user", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(USER_ID).role(role.name()).build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
