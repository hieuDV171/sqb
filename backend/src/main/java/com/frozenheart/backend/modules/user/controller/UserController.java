package com.frozenheart.backend.modules.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.user.dto.AccountDeletionResponse;
import com.frozenheart.backend.modules.user.dto.ProfileResponse;
import com.frozenheart.backend.modules.user.dto.UpdateProfileRequest;
import com.frozenheart.backend.modules.user.service.AccountDeletionService;
import com.frozenheart.backend.modules.user.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "03. Hồ sơ người dùng (User Profile)", description = "Các API tra cứu và quản lý thông tin hồ sơ cá nhân: Xem trang cá nhân, Cập nhật thông tin, Xóa tài khoản")
public class UserController {

    private final UserService userService;
    private final AccountDeletionService accountDeletionService;

    @Operation(summary = "Lấy hồ sơ cá nhân của tôi", description = "Trả về thông tin chi tiết đầy đủ của người dùng hiện tại (bao gồm email, ngày sinh, điểm gamification, danh hiệu...).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin cá nhân thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn (TOKEN_INVALID_OR_EXPIRED)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản đã bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getMyProfile() {
        ProfileResponse response = userService.getMyProfile();

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem hồ sơ công khai của người dùng khác", description = "Lấy thông tin hồ sơ của người dùng theo ID (đã ẩn các thông tin nhạy cảm như email, ngày sinh) kèm trạng thái quan hệ bạn bè/theo dõi.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy thông tin hồ sơ thành công"),
            @ApiResponse(responseCode = "403", description = "Tài khoản người dùng này đã bị khóa (ACCOUNT_NOT_ACTIVE)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)")
    })
    @GetMapping("/{userId}")
    public ResponseEntity<GlobalResponse<ProfileResponse>> getUserProfile(
            @Parameter(description = "ID người dùng cần xem", example = "169", required = true)
            @PathVariable Long userId
    ) {
        ProfileResponse response = userService.getUserProfile(userId);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Cập nhật hồ sơ cá nhân", description = "Người dùng chỉnh sửa thông tin họ tên, ảnh đại diện, ảnh bìa, ngày sinh, giới tính, tiểu sử.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật hồ sơ thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu không hợp lệ hoặc múi giờ không đúng (INVALID_PARAMETER_VALUE)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản đã bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @PutMapping("/me")
    public ResponseEntity<GlobalResponse<ProfileResponse>> updateMyProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        ProfileResponse response = userService.updateMyProfile(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
    }
    
    @Operation(summary = "Yêu cầu xóa tài khoản sau 30 ngày", description = "Tài khoản vẫn hoạt động trong thời gian chờ. Gửi lại yêu cầu giữ nguyên hạn đã đặt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Trả về thời điểm xóa đã lên lịch"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn (TOKEN_INVALID_OR_EXPIRED)"),
            @ApiResponse(responseCode = "403", description = "Tài khoản đã bị khóa (ACCOUNT_NOT_ACTIVE)")
    })
    @DeleteMapping("/me")
    public ResponseEntity<GlobalResponse<AccountDeletionResponse>> requestAccountDeletion() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        return ResponseEntity.ok(GlobalResponse.success(accountDeletionService.request(userId)));
    }

    @Operation(summary = "Xem lịch xóa tài khoản", description = "Thời điểm xóa để trống nếu không có yêu cầu đang chờ.")
    @GetMapping("/me/deletion")
    public ResponseEntity<GlobalResponse<AccountDeletionResponse>> getAccountDeletionStatus() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        return ResponseEntity.ok(GlobalResponse.success(accountDeletionService.getStatus(userId)));
    }

    @Operation(summary = "Hủy yêu cầu xóa tài khoản", description = "Có thể hủy trước hạn xóa. Gọi khi chưa có yêu cầu cũng trả về trạng thái không có lịch xóa.")
    @ApiResponse(responseCode = "409", description = "Đã hết thời hạn hủy yêu cầu xóa")
    @DeleteMapping("/me/deletion")
    public ResponseEntity<GlobalResponse<AccountDeletionResponse>> cancelAccountDeletion() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        return ResponseEntity.ok(GlobalResponse.success(accountDeletionService.cancel(userId)));
    }

}
