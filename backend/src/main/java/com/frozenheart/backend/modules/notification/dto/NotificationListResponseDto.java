package com.frozenheart.backend.modules.notification.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trang thông báo và tổng số thông báo chưa đọc")
public class NotificationListResponseDto {

    @Schema(description = "Các mục thuộc trang hiện tại; danh sách rỗng khi không có kết quả")
    private List<NotificationDto> items;

    @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
    private CursorPaginationDto pagination;

    @Schema(description = "Số mục chưa đọc của người dùng hiện tại theo ngữ cảnh API", example = "3")
    private int unreadCount;
}
