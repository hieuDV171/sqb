package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.socialinteraction.Report;
import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import com.frozenheart.backend.modules.report.dto.CreateReportRequestDto;
import com.frozenheart.backend.modules.report.repository.ReportRepository;
import com.frozenheart.backend.modules.report.service.impl.ReportServiceImpl;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ReportSubmissionTest {
    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void repeatedSubmissionReturnsOriginalPendingReportBeforeAttachingEvidence() {
        var reports = mock(ReportRepository.class);
        var users = mock(UserRepository.class);
        var targets = mock(ReportTargets.class);
        var evidence = mock(ReportEvidenceStorage.class);
        var auth = new UsernamePasswordAuthenticationToken("student", null, List.of());
        auth.setDetails(JwtPayload.builder().userId(5L).role("STUDENT").build());
        SecurityContextHolder.getContext().setAuthentication(auth);
        when(users.findByIdForUpdate(5L)).thenReturn(Optional.of(User.builder().id(5L).build()));
        when(reports.findFirstByReporterIdAndTargetTypeAndTargetIdAndStatusOrderByIdDesc(5L, "POST", 40L,
                ReportStatus.PENDING)).thenReturn(Optional.of(Report.builder().id(18L).targetId(40L).build()));

        var request = new CreateReportRequestDto();
        request.setTargetId(40L);
        request.setTargetType(ReportTargetType.POST);
        var result = new ReportServiceImpl(reports, users, targets, evidence).createReport(request);

        assertThat(result.created()).isFalse();
        assertThat(result.report().getReportId()).isEqualTo(18L);
        verifyNoInteractions(targets, evidence);
        verify(reports, never()).save(any());
    }
}
