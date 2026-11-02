package com.frozenheart.backend.modules.activityfeed.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.exception.AppException;
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

import java.time.Instant;
import java.time.format.DateTimeParseException;

@RestController
@RequiredArgsConstructor
@Tag(name = "13. Dòng hoạt động (Activity Feed)", description = "Các API đọc bảng tin dòng sự kiện cá nhân hóa và mạng lưới bạn bè (đăng bài, chia sẻ câu hỏi, video bài giảng, thành tích huy hiệu)")
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
            @ApiResponse(responseCode = "400", description = "Mốc thời gian since không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/activity-feeds/new-count")
    public ResponseEntity<GlobalResponse<NewFeedCountResponseDto>> getNewFeedCount(
            @Parameter(description = "Mốc thời gian epoch milliseconds; tạm hỗ trợ thêm ISO-8601 để tương thích client cũ", example = "1728200000000")
            @RequestParam(required = false) String since) {
        NewFeedCountResponseDto response = activityFeedService.getNewFeedCount(parseSince(since));
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    static Long parseSince(String since) {
        if (since == null || since.isBlank()) {
            return null;
        }

        String normalized = since.trim();
        try {
            return Long.valueOf(normalized);
        } catch (NumberFormatException ignored) {
            try {
                return Instant.parse(normalized).toEpochMilli();
            } catch (DateTimeParseException invalidTimestamp) {
                throw new AppException(
                        ResponseCode.INVALID_PARAMETER_VALUE,
                        "Tham số since phải là epoch milliseconds hoặc thời gian ISO-8601 hợp lệ");
            }
        }
    }
}
