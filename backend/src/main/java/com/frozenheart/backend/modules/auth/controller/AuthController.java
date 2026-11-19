package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
import com.frozenheart.backend.modules.auth.dto.user.ConfirmEmailVerificationRequest;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "01. Xác thực (Auth)", description = "Các API xác thực tài khoản: Đăng nhập, Đăng xuất, Đổi mật khẩu và Cấp mới Token")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Web: đăng nhập bằng cookie HttpOnly", description = "Chỉ platform WEB, kể cả trình duyệt điện thoại/Flutter Web/PWA. JSON trả access_token; refresh_token null, RT chỉ trong cookie HttpOnly/Secure/SameSite Strict. Giữ AT trong memory. Native dùng /auth/native/login.")
    @SecurityRequirements // Public endpoint - không yêu cầu token trước khi gọi
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Thông tin user và AT trong JSON; RT chỉ ở cookie với tài khoản ACTIVE"),
            @ApiResponse(responseCode = "400", description = "Sai tài khoản hoặc mật khẩu (INCORRECT_IDENTIFIER)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Web: đăng xuất và xóa cookie", description = "Gửi Bearer AT WEB. Thu hồi refresh token/thiết bị và xóa cookie RT. Native dùng /auth/native/logout.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng xuất thành công khỏi thiết bị hiện tại"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn (TOKEN_INVALID_OR_EXPIRED)")
    })
    @PostMapping("/logout")
    public ResponseEntity<GlobalResponse<Void>> logout() {

        authService.logout();

        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Đổi mật khẩu người dùng", description = "Người dùng tự cập nhật mật khẩu mới. Yêu cầu nhập đúng mật khẩu cũ và mật khẩu mới.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đổi mật khẩu thành công"),
            @ApiResponse(responseCode = "400", description = "Mật khẩu cũ không chính xác (INCORRECT_IDENTIFIER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn (TOKEN_INVALID_OR_EXPIRED)")
    })
    @PostMapping("/change-password")
    public ResponseEntity<GlobalResponse<Void>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Gửi mã xác thực email", description = "Gửi OTP tới email của tài khoản đang đăng nhập. Email được lấy từ JWT, không nhận từ client.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã gửi mã OTP"),
            @ApiResponse(responseCode = "409", description = "Email đã xác thực hoặc đang trong thời gian chờ gửi lại")
    })
    @PostMapping("/email-verification/request")
    public ResponseEntity<GlobalResponse<Void>> requestEmailVerification() {
        authService.requestEmailVerification();
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Xác nhận email", description = "Xác thực OTP cho chính tài khoản đang đăng nhập và cập nhật verified = true.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác thực email thành công"),
            @ApiResponse(responseCode = "400", description = "OTP không đúng hoặc đã hết hạn")
    })
    @PostMapping("/email-verification/confirm")
    public ResponseEntity<GlobalResponse<Void>> confirmEmailVerification(
            @Valid @RequestBody ConfirmEmailVerificationRequest request
    ) {
        authService.confirmEmailVerification(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Web: refresh bằng cookie HttpOnly", description = "Không gửi refresh_token trong body; trình duyệt gửi credentials/cookie. Chỉ RT WEB hoặc cookie legacy được nhận. JSON trả AT mới, RT mới chỉ trong Set-Cookie. Native dùng /auth/native/refresh.")
    @SecurityRequirements // Public endpoint
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cấp mới Access Token thành công"),
            @ApiResponse(responseCode = "401", description = "Refresh Token không hợp lệ hoặc đã hết hạn (TOKEN_INVALID_OR_EXPIRED)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản đã bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @PostMapping("/refresh")
    public ResponseEntity<GlobalResponse<RefreshTokenResponse>> refreshToken(
            @Valid @RequestBody(required = false) RefreshTokenRequest request
    ) {
        RefreshTokenResponse response = authService.refreshToken(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
