package com.frozenheart.backend.modules.auth.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.auth.dto.admin.AuthResponse;
import com.frozenheart.backend.modules.auth.dto.user.LoginRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenRequest;
import com.frozenheart.backend.modules.auth.dto.user.RefreshTokenResponse;
import com.frozenheart.backend.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth/native")
@RequiredArgsConstructor
@Tag(name = "01b. Xác thực Native / Flutter", description = "Flutter native Android/iOS và desktop: AT/RT qua JSON, RT lưu trong secure storage. Flutter Web/PWA dùng luồng WEB. API nghiệp vụ dùng chung Bearer AT. Endpoint/platform không chứng minh danh tính ứng dụng.")
public class NativeAuthController {
    private final AuthService authService;

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Native: đăng nhập nhận AT/RT JSON", description = "Gửi email, password, device_id ổn định và platform ANDROID/IOS/DESKTOP. Server gắn kênh NATIVE vào JWT. Tài khoản chưa hoàn tất thiết lập chỉ nhận ONBOARDING AT, không RT; hoàn tất thiết lập rồi đăng nhập lại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AT/RT JSON cho ACTIVE; ONBOARDING chỉ có AT giới hạn"),
            @ApiResponse(responseCode = "400", description = "Sai credential, platform hoặc thiếu trường bắt buộc"),
            @ApiResponse(responseCode = "403", description = "Tài khoản bị khóa/đình chỉ")
    })
    public ResponseEntity<GlobalResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(authService.loginNative(request)));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Native: xoay refresh token qua JSON", description = "Body {\"refresh_token\":\"...\"}. Chỉ nhận RT NATIVE, không đọc cookie. Thay cả AT/RT bằng cặp mới sau mỗi lần thành công và gom request refresh đồng thời. RT WEB hoặc RT legacy thiếu kênh bị từ chối; đăng nhập lại native/login.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cặp AT/RT mới, RT cũ không được dùng lại"),
            @ApiResponse(responseCode = "400", description = "Thiếu hoặc rỗng refresh_token"),
            @ApiResponse(responseCode = "401", description = "RT hết hạn, sai kênh, đã xoay/thu hồi hoặc phiên không còn hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Tài khoản không còn được phép dùng phiên đầy đủ")
    })
    public ResponseEntity<GlobalResponse<RefreshTokenResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(GlobalResponse.success(authService.refreshNativeToken(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Native: đăng xuất phiên hiện tại", description = "Gửi Bearer AT NATIVE; thu hồi RT/thiết bị rồi app xóa token đã lưu. AT hết hạn thì refresh trước. Không thay đổi cookie WEB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã thu hồi RT/thiết bị; data null, client bỏ token đã lưu"),
            @ApiResponse(responseCode = "401", description = "AT hết hạn/sai kênh hoặc chưa đăng nhập")
    })
    public ResponseEntity<GlobalResponse<Void>> logout() {
        authService.logoutNative();
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
