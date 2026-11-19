package com.frozenheart.backend.modules.conversation.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.util.List;
import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Trang lịch sử tin nhắn mà thành viên hiện tại được phép xem")
public class MessageListResponseDto {

    @Schema(description = "ID cuộc hội thoại mà người gọi là thành viên hoặc được phép truy cập", example = "42")
    private Long conversationId;

    @Schema(description = "Loại cuộc hội thoại theo enum API")
    private ConversationType conversationType;
    @Schema(description = "Tên nhóm trò chuyện", example = "Nhóm ôn tập Cơ sở dữ liệu")
    private String groupName;
    @Schema(description = "URL ảnh đại diện nhóm, có thể null nếu nhóm chưa có ảnh", example = "https://media.example.com/group.png")
    private String groupAvatarUrl;

    @Schema(description = "Thời điểm ẩn hoặc mốc lịch sử bị ẩn theo ngữ cảnh hội thoại; có thể null", example = "2026-11-19T07:00:00Z")
    private Instant hiddenAt;

    @Schema(description = "Các tin nhắn trong trang lịch sử hiện tại")
    private List<MessageDto> messages;

    @Schema(description = "Mốc và cờ phân trang; không tự suy đoán cursor từ thứ tự hiển thị")
    private CursorPaginationDto pagination;
}
