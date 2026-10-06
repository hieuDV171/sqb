package com.frozenheart.backend.modules.activityfeed.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.activityfeed.dto.*;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "12. Dòng hoạt động (Activity Feed)", description = "Các API đọc bảng tin dòng sự kiện cá nhân hóa và mạng lưới bạn bè (đăng bài, chia sẻ câu hỏi, video bài giảng, thành tích huy hiệu)")
public class ActivityFeedController {

    private final ActivityFeedService activityFeedService;

    @Operation(summary = "Lấy dòng hoạt động cá nhân hóa", description = "Trả về bảng tin sự kiện được cá nhân hóa theo mạng lưới bạn bè và người đang theo dõi của người dùng hiện tại (có loại trừ người bị chặn).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy dòng hoạt động thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/activity-feeds")
    public ResponseEntity<GlobalResponse<CursorResponse<ActivityFeedItemDto>>> getActivityFeeds(
            @Parameter(description = "Con trỏ ID hoạt động để lấy sự kiện cũ hơn", example = "500")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng sự kiện mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        CursorResponse<ActivityFeedItemDto> response = activityFeedService.getActivityFeeds(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Đếm số lượng hoạt động mới", description = "Kiểm tra số lượng sự kiện mới phát sinh kể từ mốc thời gian chỉ định (thường dùng để hiển thị badge 'Có tin mới' trên giao diện).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đếm số hoạt động mới thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/activity-feeds/new-count")
    public ResponseEntity<GlobalResponse<NewFeedCountResponseDto>> getNewFeedCount(
            @Parameter(description = "Mốc thời gian epoch milliseconds", example = "1728200000000")
            @RequestParam(required = false) Long since) {
        NewFeedCountResponseDto response = activityFeedService.getNewFeedCount(since);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
