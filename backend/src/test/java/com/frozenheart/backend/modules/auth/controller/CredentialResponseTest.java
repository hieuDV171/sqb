package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.core.dto.IssuedAccountCredential;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.*;
import com.frozenheart.backend.modules.auth.dto.admin.AdminResetPasswordRequest;
import com.frozenheart.backend.modules.auth.service.AuthService;
import com.frozenheart.backend.modules.session.controller.AdminCourseClassController;
import com.frozenheart.backend.modules.session.dto.ExcelImportClassResult;
import com.frozenheart.backend.modules.session.service.CourseClassManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class CredentialResponseTest {
    @Test void credentialResponsesAreNeverCacheable() {
        var auth = mock(AuthService.class);
        var admin = new AdminController(auth);
        var credential = new IssuedAccountCredential(1L, "fixture@example.edu", "Sinh viên", "00123", "Fixture-1234!Aa");
        var bulk = new BulkImportRequest(List.of());
        when(auth.bulkImportUsers(bulk)).thenReturn(new BulkImportResult(1, 0, List.of(), List.of(credential)));
        assertThat(admin.bulkImportUsers(bulk).getHeaders().getCacheControl()).isEqualTo("no-store");
        var reset = new AdminResetPasswordRequest(credential.email(), null);
        when(auth.adminResetPassword(reset)).thenReturn(credential);
        assertThat(admin.adminResetPassword(reset).getHeaders().getCacheControl()).isEqualTo("no-store");
        var courses = mock(CourseClassManagementService.class);
        var file = mock(MultipartFile.class);
        when(courses.importAndEnrollFromExcel(file, null)).thenReturn(ExcelImportClassResult.builder().credentials(List.of(credential)).build());
        assertThat(new AdminCourseClassController(courses).importCourseClassFromExcel(file, null)
                .getHeaders().getCacheControl()).isEqualTo("no-store");
    }
}
