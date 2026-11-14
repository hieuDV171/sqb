package com.frozenheart.backend.modules.exam.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.Exam;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.dto.ExportResponse;
import com.frozenheart.backend.modules.exam.repository.ExamRepository;
import com.frozenheart.backend.modules.exam.service.DocumentExportService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExamServiceImplTest {

    private static final long OWNER_ID = 7L;
    private static final long EXAM_ID = 19L;

    @Mock private ExamRepository examRepository;
    @Mock private DocumentExportService documentExportService;
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
        when(documentExportService.exportExam(exam, "pdf", true, "A4")).thenReturn(exported);

        assertEquals(EXAM_ID, service.getExamDetail(EXAM_ID).getExamId());
        assertSame(exported, service.exportExam(EXAM_ID, "pdf", true, "A4"));
    }

    private void assertAccessDenied() {
        assertEquals(ResponseCode.ACCESS_DENIED,
                assertThrows(AppException.class, () -> service.getExamDetail(EXAM_ID)).getResponseCode());
        assertEquals(ResponseCode.ACCESS_DENIED,
                assertThrows(AppException.class, () -> service.exportExam(EXAM_ID, "pdf", true, "A4")).getResponseCode());
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
