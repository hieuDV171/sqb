package com.frozenheart.backend.modules.block.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Danh sách tài khoản bị người dùng hiện tại chặn, có cursor tiếp trang")
public class BlockedListResponseDto {
    @Schema(description = "Các mục thuộc trang hiện tại; danh sách rỗng khi không có kết quả")
    private List<BlockedUserDto> items;
    @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
    private CursorPaginationDto pagination;
    @Schema(description = "Cursor thời gian và ID cho trang tiếp theo; gửi nguyên chuỗi, null khi hết trang", example = "2026-11-19T00:00:00Z|42")
    private String nextCursor;

    @Schema(description = "Tổng số tài khoản người dùng hiện tại đã chặn", example = "3")
    private int totalBlocked;
}
