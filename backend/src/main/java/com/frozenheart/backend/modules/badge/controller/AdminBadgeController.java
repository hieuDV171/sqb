package com.frozenheart.backend.modules.badge.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeAccountResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeResponseDto;
import com.frozenheart.backend.modules.badge.dto.CreateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.ManualGrantBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.UpdateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.RevokeBadgeRequest;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "22. Huy hiệu & Danh hiệu (Badges)", description = "APIs quản lý và cấu hình huy hiệu dành cho Quản trị viên")
@RestController
@RequestMapping("/admin/badges")
@RequiredArgsConstructor
public class AdminBadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "Lấy toàn bộ danh sách huy hiệu (Quản trị viên)", description = "Trả về tất cả các huy hiệu bao gồm cả các huy hiệu đang hoạt động và đã bị vô hiệu hóa.")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách huy hiệu thành công")
    @GetMapping
    public ResponseEntity<GlobalResponse<List<BadgeResponseDto>>> getAllBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getAdminBadges()));
    }

    @Operation(summary = "Tạo mới huy hiệu danh hiệu", description = "Tạo một huy hiệu mới với tên, cấp bậc (BRONZE/SILVER/GOLD/PLATINUM/DIAMOND), sự kiện kích hoạt và điều kiện criteria.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Tạo huy hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "409", description = "Tên huy hiệu đã tồn tại (BADGE_NAME_ALREADY_EXISTS)")
    })
    @PostMapping
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> createBadge(@Valid @RequestBody CreateBadgeRequestDto request) {
        BadgeResponseDto created = badgeService.createBadge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(created));
    }

    @Operation(summary = "Cập nhật thông tin huy hiệu", description = "Chỉnh sửa tên, mô tả, cấp bậc, ảnh đại diện, tiêu chí hoặc kích hoạt lại huy hiệu.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cập nhật huy hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Huy hiệu không tồn tại (BADGE_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Tên huy hiệu mới bị trùng lặp (BADGE_NAME_ALREADY_EXISTS)")
    })
    @PutMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> updateBadge(
            @Parameter(description = "ID huy hiệu cần cập nhật", example = "1") @PathVariable Long badgeId,
            @Valid @RequestBody UpdateBadgeRequestDto request
    ) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.updateBadge(badgeId, request)));
    }

    @Operation(summary = "Vô hiệu hóa (Soft Delete) huy hiệu", description = "Đánh dấu huy hiệu thành không hoạt động (active = false) để người dùng không thể nhận mới nhưng vẫn giữ lại cho những ai đã nhận trước đó.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Vô hiệu hóa huy hiệu thành công"),
            @ApiResponse(responseCode = "404", description = "Huy hiệu không tồn tại (BADGE_NOT_FOUND)")
    })
    @DeleteMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<Void>> deleteBadge(
            @Parameter(description = "ID huy hiệu cần xóa/vô hiệu hóa", example = "1") @PathVariable Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.ok(GlobalResponse.success(null));
    }

    @Operation(summary = "Cấp phát huy hiệu thủ công cho người dùng", description = "Quản trị viên có thể trao mọi huy hiệu đang hoạt động cho danh sách user_ids, kể cả huy hiệu có xét thưởng tự động. Người đã đạt huy hiệu được bỏ qua.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cấp phát huy hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "403", description = "Huy hiệu đã ngừng hoạt động hoặc không có quyền quản trị"),
            @ApiResponse(responseCode = "404", description = "Huy hiệu hoặc tài khoản nhận không tồn tại")
    })
    @PostMapping("/{badgeId}/grant")
    public ResponseEntity<GlobalResponse<Void>> grantManualBadge(
            @Parameter(description = "ID huy hiệu cần cấp phát", example = "1") @PathVariable Long badgeId,
            @Valid @RequestBody ManualGrantBadgeRequestDto request
    ) {
        badgeService.grantManualBadge(badgeId, request);
        return ResponseEntity.ok(GlobalResponse.success(null));
    }

    @Operation(summary = "Tìm tài khoản để trao hoặc thu hồi huy hiệu", description = "ADMIN tìm theo tên/email, tối đa 100 ký tự. holders=true chỉ người đang giữ huy hiệu, false chọn tài khoản hoạt động chưa giữ để trao. Mỗi trang tối đa 20 tài khoản theo ID tăng dần; gửi pagination.after cho trang kế tiếp. Huy hiệu không tồn tại trả 404.")
    @ApiResponse(responseCode = "200", description = "Trang tài khoản phù hợp với bộ lọc")
    @GetMapping("/{badgeId}/accounts")
    public GlobalResponse<CursorResponse<BadgeAccountResponse>> searchBadgeAccounts(@Parameter(description = "ID huy hiệu cần tìm người giữ/trao/thu hồi", example = "42") @PathVariable Long badgeId,
            @Parameter(description = "true để tìm người đang giữ huy hiệu, false để tìm người chưa giữ; mặc định false", example = "false") @RequestParam(defaultValue = "false") boolean holders,
            @Parameter(description = "Từ khóa tên/email, tối đa 100 ký tự; bỏ trống để không lọc", example = "Anh") @RequestParam(defaultValue = "") String query,
            @Parameter(description = "ID cursor từ pagination.after của trang trước; trang đầu bỏ trống", example = "42") @RequestParam(required = false) Long after) {
        return GlobalResponse.success(badgeService.searchBadgeAccounts(badgeId, holders, query, after));
    }

    @Operation(summary = "Thu hồi huy hiệu của các tài khoản đã chọn", description = "ADMIN thu hồi các lượt trao đang hiệu lực cho user_ids, giảm thống kê huy hiệu và gửi thông báo người bị thu hồi. Tài khoản không đang giữ huy hiệu được bỏ qua; giữ lịch sử trao/thu hồi. Gọi lại không trừ số lượng nhiều lần.")
    @ApiResponse(responseCode = "200", description = "Đã thu hồi các lượt trao còn hiệu lực; data null")
    @PostMapping("/{badgeId}/revoke")
    public GlobalResponse<Void> revokeBadge(@Parameter(description = "ID huy hiệu cần tìm người giữ/trao/thu hồi", example = "42") @PathVariable Long badgeId, @Valid @RequestBody RevokeBadgeRequest request) {
        badgeService.revokeBadge(badgeId, request.userIds());
        return GlobalResponse.success();
    }
}
