package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trang hội thoại của tài khoản hiện tại với cursor thời gian ổn định")
public class ConversationListResponseDto {

    @Schema(description = "Các mục thuộc trang hiện tại; danh sách rỗng khi không có kết quả")
    private List<ConversationDto> items;

    @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
    private CursorPaginationDto pagination;

    @Schema(description = "Cursor thời gian và ID cho trang tiếp theo; gửi nguyên chuỗi, null khi hết trang", example = "2026-11-19T00:00:00Z|42")
    private String nextCursor;

    @Schema(description = "Tổng số hội thoại trong phạm vi danh sách", example = "3")
    private int totalConversations;
}
