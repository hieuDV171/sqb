package com.frozenheart.backend.modules.badge.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeResponseDto;
import com.frozenheart.backend.modules.badge.dto.UserBadgeResponseDto;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "21. Huy hiệu & Danh hiệu (Badges)", description = "APIs xem danh sách và huy hiệu đã đạt của người dùng")
@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @Operation(summary = "Lấy danh sách tất cả huy hiệu đang hoạt động trong hệ thống", description = "Trả về danh sách các huy hiệu có trạng thái active = true, sắp xếp theo ID tăng dần.")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách huy hiệu thành công")
    @GetMapping
    public ResponseEntity<GlobalResponse<List<BadgeResponseDto>>> getAllBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getAllBadges()));
    }

    @Operation(summary = "Lấy danh sách huy hiệu của chính người dùng hiện tại", description = "Trả về danh sách tất cả các huy hiệu mà người dùng đang đăng nhập đã đạt được.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy danh sách huy hiệu của bản thân thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực người dùng")
    })
    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<List<UserBadgeResponseDto>>> getMyBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getMyBadges()));
    }

    @Operation(summary = "Lấy danh sách huy hiệu của người dùng khác", description = "Xem bộ sưu tập huy hiệu trên hồ sơ cá nhân của người dùng được chỉ định theo userId.")
    @ApiResponse(responseCode = "200", description = "Lấy danh sách huy hiệu người dùng thành công")
    @GetMapping("/users/{userId}")
    public ResponseEntity<GlobalResponse<List<UserBadgeResponseDto>>> getUserBadges(
            @Parameter(description = "ID của người dùng cần xem huy hiệu", example = "10") @PathVariable Long userId) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getUserBadges(userId)));
    }

    @Operation(summary = "Xem chi tiết một huy hiệu", description = "Lấy thông tin chi tiết, cấp bậc, tiêu chí và tổng số người đã đạt của một huy hiệu.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết huy hiệu thành công"),
            @ApiResponse(responseCode = "404", description = "Huy hiệu không tồn tại (BADGE_NOT_FOUND)")
    })
    @GetMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> getBadgeById(
            @Parameter(description = "ID của huy hiệu", example = "1") @PathVariable Long badgeId) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getBadgeById(badgeId)));
    }
}
