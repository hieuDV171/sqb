package com.frozenheart.backend.modules.auth.service;

import com.frozenheart.backend.modules.auth.dto.admin.AdminResetPasswordRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.admin.RegisterRequest;
import com.frozenheart.backend.modules.auth.dto.admin.ResendOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.VerifyOtpRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportRequest;
import com.frozenheart.backend.modules.auth.dto.admin.AdminBulkCreateUserDto.BulkImportResult;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;

public interface AuthService {
    void register(RegisterRequest request);

    AuthResponse verify(VerifyOtpRequest request);

    void resendVerify(ResendOtpRequest request);

    BulkImportResult bulkImportUsers(BulkImportRequest request);

    AuthResponse login(LoginRequest request);

    void logout();

    void changePassword(ChangePasswordRequest request);

    RefreshTokenResponse refreshToken(RefreshTokenRequest request);

    void adminResetPassword(AdminResetPasswordRequest request);
}
