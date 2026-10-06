package com.frozenheart.backend.modules.auth.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "02. Quản trị Tài khoản (Admin Auth)", description = "Các API dành riêng cho Quản trị viên: Đăng ký tài khoản mới, Xác thực OTP, Tạo người dùng hàng loạt, Khôi phục mật khẩu")
public class AdminController {

    private final AuthService authService;

    @Operation(summary = "Đăng ký tài khoản (Admin)", description = "Tạo một tài khoản mới và gửi mã xác thực OTP 6 số về hòm thư điện tử.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng ký thành công, vui lòng kiểm tra email nhận mã OTP"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "409", description = "Email đã tồn tại trong hệ thống (USER_ALREADY_EXISTS)")
    })
    @PostMapping("/register")
    public ResponseEntity<GlobalResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);

        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Xác thực kích hoạt tài khoản bằng mã OTP", description = "Xác nhận mã OTP được gửi tới email để kích hoạt tài khoản (verified = true) và nhận token đăng nhập.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xác thực OTP thành công, trả về token đăng nhập"),
            @ApiResponse(responseCode = "400", description = "Mã OTP không chính xác hoặc đã hết hiệu lực (VERIFICATION_CODE_INVALID)")
    })
    @PostMapping("/verify")
    public ResponseEntity<GlobalResponse<AuthResponse>> verify(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse response = authService.verify(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Gửi lại mã xác thực OTP", description = "Cấp phát và gửi lại mã OTP mới qua email nếu mã cũ hết hạn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã gửi lại mã OTP thành công"),
            @ApiResponse(responseCode = "409", description = "Mã OTP trước đó vẫn còn hiệu lực hoặc đang trong thời gian chờ (ACTION_ALREADY_PERFORMED)")
    })
    @PostMapping("/resend-verify")
    public ResponseEntity<GlobalResponse<Void>> resendVerify(@Valid @RequestBody ResendOtpRequest request) {
        authService.resendVerify(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Tạo hàng loạt tài khoản người dùng (Bulk Import)", description = "Quản trị viên tạo danh sách nhiều tài khoản (Sinh viên, Giảng viên, Admin). Tự động sinh mật khẩu BCrypt theo quy tắc an toàn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xử lý thành công, trả về số lượng tạo thành công và danh sách lỗi nếu có"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Quản trị viên (ACCESS_DENIED)")
    })
    @PostMapping("/bulk-import")
    public ResponseEntity<GlobalResponse<BulkImportResult>> bulkImportUsers(
            @Valid @RequestBody BulkImportRequest request) {
        BulkImportResult result = authService.bulkImportUsers(request);

        return ResponseEntity.ok(GlobalResponse.success(result));
    }

    @Operation(summary = "Đặt lại mật khẩu cho người dùng (Reset Password)", description = "Quản trị viên đặt lại mật khẩu cho tài khoản người dùng về mật khẩu chỉ định hoặc mật khẩu mặc định của trường. Tự động xóa sạch Refresh Token trên Redis để đăng xuất khỏi tất cả thiết bị.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đặt lại mật khẩu thành công"),
            @ApiResponse(responseCode = "403", description = "Không có quyền Quản trị viên (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Email người dùng không tồn tại (USER_NOT_FOUND)")
    })
    @PostMapping("/reset-password")
    public ResponseEntity<GlobalResponse<Void>> adminResetPassword(
            @Valid @RequestBody AdminResetPasswordRequest request
        ) {
            authService.adminResetPassword(request);
            return ResponseEntity.ok(GlobalResponse.success());
    }

}
