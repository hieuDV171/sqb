package com.frozenheart.backend.modules.auth.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.auth.dto.admin.AdminResetPasswordRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.admin.RegisterRequest;
import com.frozenheart.backend.modules.auth.dto.admin.ResendOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.VerifyOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportResult;
import com.frozenheart.backend.modules.auth.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth/admin")
public class AdminController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PostMapping("/verify")
    public ResponseEntity<GlobalResponse<AuthResponse>> verify(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verify(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/resend-verify")
    public ResponseEntity<GlobalResponse<Void>> resendVerify(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendVerify(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PostMapping("/bulk-import")
    public ResponseEntity<GlobalResponse<BulkImportResult>> bulkImportUsers(
            @Valid @RequestBody BulkImportRequest request) {
        BulkImportResult result = authService.bulkImportUsers(request);

        return ResponseEntity.ok(GlobalResponse.success(result));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<GlobalResponse<Void>> adminResetPassword(
            @Valid @RequestBody AdminResetPasswordRequest request
        ) {
            authService.adminResetPassword(request);
            return ResponseEntity.ok(GlobalResponse.success());
    }

}
