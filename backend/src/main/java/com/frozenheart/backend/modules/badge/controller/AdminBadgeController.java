package com.frozenheart.backend.modules.badge.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeResponseDto;
import com.frozenheart.backend.modules.badge.dto.CreateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.ManualGrantBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.UpdateBadgeRequestDto;
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

@Tag(name = "21. Huy hiệu & Danh hiệu (Badges)", description = "APIs quản lý và cấu hình huy hiệu dành cho Quản trị viên")
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

    @Operation(summary = "Cấp phát huy hiệu thủ công cho người dùng", description = "Quản trị viên trao tặng huy hiệu trực tiếp cho danh sách userIds mà không cần qua điều kiện trigger tự động.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cấp phát huy hiệu thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "404", description = "Huy hiệu không tồn tại (BADGE_NOT_FOUND)")
    })
    @PostMapping("/{badgeId}/grant")
    public ResponseEntity<GlobalResponse<Void>> grantManualBadge(
            @Parameter(description = "ID huy hiệu cần cấp phát", example = "1") @PathVariable Long badgeId,
            @Valid @RequestBody ManualGrantBadgeRequestDto request
    ) {
        badgeService.grantManualBadge(badgeId, request);
        return ResponseEntity.ok(GlobalResponse.success(null));
    }
}
