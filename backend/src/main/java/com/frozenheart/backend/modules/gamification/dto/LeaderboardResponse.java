package com.frozenheart.backend.modules.gamification.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Schema(description = "Phản hồi Bảng xếp hạng vinh danh sinh viên")
@Builder
public record LeaderboardResponse(
        @Schema(description = "Danh sách người dùng trên bảng xếp hạng")
        List<LeaderboardEntryDto> entries,

        @Schema(description = "Thông tin thứ hạng của người dùng hiện tại")
        MyRankDto myRank,

        @Schema(description = "Thông tin phân trang dạng con trỏ")
        CursorPaginationDto pagination
) {}

