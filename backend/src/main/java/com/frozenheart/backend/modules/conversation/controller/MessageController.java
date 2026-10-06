package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.conversation.dto.DeleteMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.DeleteMessageResponseDto;
import com.frozenheart.backend.modules.conversation.dto.MessageListResponseDto;
import com.frozenheart.backend.modules.conversation.service.MessageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "10. Trò chuyện - Tin nhắn (Messages)", description = "Các API đọc lịch sử tin nhắn, tự động cập nhật biên nhận đã đọc (Read Receipt) và thu hồi tin nhắn (ME vs EVERYONE)")
public class MessageController {

    private final MessageService messageService;

    @Operation(summary = "Lấy lịch sử tin nhắn trong cuộc hội thoại", description = "Đọc tin nhắn theo phân trang con trỏ (cursor pagination). Tự động cập nhật biên nhận đã đọc (Read Receipt) cho người xem.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách tin nhắn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Bạn không tham gia cuộc hội thoại này (ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc hội thoại (CONVERSATION_NOT_FOUND)")
    })
    @GetMapping("/conversations/{id}/messages")
    public ResponseEntity<GlobalResponse<MessageListResponseDto>> getMessages(
            @Parameter(description = "ID cuộc hội thoại", example = "10", required = true)
            @PathVariable("id") Long conversationId,
            @Parameter(description = "Con trỏ ID tin nhắn để lấy tin nhắn cũ hơn", example = "1000")
            @RequestParam(value = "after", required = false) Long after,
            @Parameter(description = "Số lượng tin nhắn mỗi trang (mặc định 20, tối đa 100)", example = "20")
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        MessageListResponseDto response = messageService.getMessages(conversationId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xóa hoặc thu hồi tin nhắn", description = "Hỗ trợ 2 chế độ: ME (chỉ xóa/ẩn ở phía người dùng hiện tại) hoặc EVERYONE (thu hồi với toàn bộ thành viên, chỉ người gửi mới có quyền).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa hoặc thu hồi tin nhắn thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền thu hồi tin nhắn của người khác hoặc không thuộc hội thoại (NOT_MESSAGE_OWNER / ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy tin nhắn (MESSAGE_NOT_FOUND)")
    })
    @DeleteMapping("/messages/{id}")
    public ResponseEntity<GlobalResponse<DeleteMessageResponseDto>> deleteMessage(
            @Parameter(description = "ID tin nhắn cần xóa/thu hồi", example = "501", required = true)
            @PathVariable("id") Long messageId,
            @Valid @RequestBody DeleteMessageRequestDto request) {
        DeleteMessageResponseDto response = messageService.deleteMessage(messageId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
