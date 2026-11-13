package com.frozenheart.backend.modules.session.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import java.sql.SQLException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class SemesterManagementServiceImplTest {
    private final SemesterRepository semesters = mock(SemesterRepository.class);
    private final SemesterManagementServiceImpl service = new SemesterManagementServiceImpl(
            semesters, mock(CourseClassRepository.class));

    @Test
    void anotherActiveSemesterReturnsConflictBeforeUpdating() {
        when(semesters.findByActiveTrue()).thenReturn(Optional.of(semester(1L, true, false)));

        assertEquals(ResponseCode.ACTIVE_SEMESTER_CONFLICT,
                assertThrows(AppException.class, () -> service.activateSemester(2L)).getResponseCode());
        verify(semesters, never()).findByIdForUpdate(2L);
    }

    @Test
    void concurrentActivationConflictFromTheUniqueIndexIsTranslated() {
        when(semesters.findByIdForUpdate(2L)).thenReturn(Optional.of(semester(2L, false, false)));
        doThrow(uniqueViolation("idx_semesters_single_active")).when(semesters).flush();

        assertEquals(ResponseCode.ACTIVE_SEMESTER_CONFLICT,
                assertThrows(AppException.class, () -> service.activateSemester(2L)).getResponseCode());
    }

    @Test
    void unrelatedIntegrityFailureKeepsItsOriginalError() {
        when(semesters.findByIdForUpdate(2L)).thenReturn(Optional.of(semester(2L, false, false)));
        var failure = uniqueViolation("another_index");
        doThrow(failure).when(semesters).flush();

        assertSame(failure, assertThrows(DataIntegrityViolationException.class, () -> service.activateSemester(2L)));
    }

    @Test
    void twoRequestsForTheSameSemesterCanSucceedIdempotently() {
        when(semesters.findByIdForUpdate(2L)).thenReturn(Optional.of(semester(2L, true, false)));

        assertEquals(2L, service.activateSemester(2L).semesterId());
        verify(semesters, never()).flush();
    }

    @Test
    void closeRequiresACompletedFinalization() {
        var open = semester(1L, true, false);
        when(semesters.findActiveForUpdate()).thenReturn(Optional.of(open));

        assertEquals(ResponseCode.ACTION_NOT_ALLOWED,
                assertThrows(AppException.class, service::deactivateAllSemesters).getResponseCode());
        open.setFinalized(true);
        service.deactivateAllSemesters();
        assertFalse(open.isActive());
    }

    private static Semester semester(Long id, boolean active, boolean finalized) {
        return Semester.builder().id(id).name("2026" + id).active(active).isFinalized(finalized).build();
    }

    private static DataIntegrityViolationException uniqueViolation(String index) {
        return new DataIntegrityViolationException("Duplicate",
                new ConstraintViolationException("Duplicate", new SQLException("Duplicate", "23505"), index));
    }
}
