package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.user.ChangePasswordRequest;
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

    @Operation(summary = "Đăng nhập hệ thống", description = "Xác thực tài khoản bằng Email và Mật khẩu. Hỗ trợ đa nền tảng (Web, Android, iOS), tự động lưu thông tin thiết bị và sinh cặp mã JWT Access Token & Refresh Token.")
    @SecurityRequirements // Public endpoint - không yêu cầu token trước khi gọi
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng nhập thành công, trả về thông tin user và cặp token"),
            @ApiResponse(responseCode = "400", description = "Sai tài khoản hoặc mật khẩu (INCORRECT_IDENTIFIER)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa (ACCOUNT_NOT_ACTIVE) hoặc chưa kích hoạt (USER_NOT_VERIFIED)")
    })
    @PostMapping("/login")
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đăng xuất tài khoản", description = "Xóa phiên đăng nhập hiện tại trên thiết bị, thu hồi Refresh Token trong Redis và vô hiệu hóa FCM Token đẩy thông báo.")
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

    @Operation(summary = "Cấp mới Access Token (Refresh)", description = "Sử dụng Refresh Token còn hiệu lực để cấp phát lại Access Token mới mà không cần người dùng nhập lại mật khẩu.")
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
