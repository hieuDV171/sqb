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
import com.frozenheart.backend.core.entity.user.DevicePlatform;
import com.frozenheart.backend.core.security.AuthCompatibilityPolicy;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "01. Xác thực (Auth)", description = "Các API xác thực tài khoản: Đăng nhập, Đăng xuất, Đổi mật khẩu và Cấp mới Token")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "Web: đăng nhập; có chế độ JSON tương thích tạm thời", description = "WEB nhận AT JSON, RT cookie HttpOnly/Secure/SameSite Strict. Khi AUTH_LEGACY_JSON_ENABLED=true, endpoint cũ còn nhận ANDROID/IOS/DESKTOP và trả RT JSON cho client chưa chuyển đổi, message thành công có cảnh báo. Khi tắt, non-WEB bị từ chối. Native mới dùng /auth/native/login. Xem trạng thái thực ở đầu Swagger.")
    @SecurityRequirements // Public endpoint - không yêu cầu token trước khi gọi
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "WEB: RT cookie, JSON RT null. Luồng tương thích non-WEB khi bật: RT JSON + message cảnh báo. ONBOARDING không RT."),
            @ApiResponse(responseCode = "400", description = "Sai tài khoản hoặc mật khẩu (INCORRECT_IDENTIFIER)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(request.platform() != DevicePlatform.WEB
                ? GlobalResponse.success(AuthCompatibilityPolicy.WARNING_MESSAGE, response)
                : GlobalResponse.success(response));
    }

    @Operation(summary = "Web: đăng xuất và xóa cookie", description = "Gửi Bearer AT WEB để thu hồi RT/thiết bị và xóa cookie. AT LEGACY_JSON dùng endpoint này để thu hồi phiên JSON, không sửa cookie, kể cả sau khi tắt chế độ tương thích. Native dùng /auth/native/logout.")
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

    @Operation(summary = "Web: refresh cookie; có chế độ JSON tương thích tạm thời", description = "Luồng chuẩn: không body, RT WEB/legacy trong cookie, RT mới chỉ Set-Cookie. Khi AUTH_LEGACY_JSON_ENABLED=true: cookie luôn ưu tiên; nếu không có cookie, nhận body refresh_token thuộc LEGACY_JSON hoặc RT cũ thiếu kênh, rotate trả AT/RT JSON với message cảnh báo. RT đã ký WEB/NATIVE không đổi sang LEGACY_JSON. Khi tắt, mọi body RT tại endpoint này bị từ chối. Native mới dùng /auth/native/refresh.")
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
        return ResponseEntity.ok(response.refreshToken() != null
                ? GlobalResponse.success(AuthCompatibilityPolicy.WARNING_MESSAGE, response)
                : GlobalResponse.success(response));
    }

}
