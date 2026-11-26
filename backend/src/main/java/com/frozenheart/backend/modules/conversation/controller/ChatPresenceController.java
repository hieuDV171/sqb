package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.conversation.dto.ChatPresenceDto;
import com.frozenheart.backend.modules.conversation.service.ChatPresenceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequiredArgsConstructor
@Tag(name = "10. Trò chuyện - Cuộc hội thoại (Conversations)", description = "Danh sách và trạng thái của các cuộc hội thoại")
public class ChatPresenceController {
    private final ChatPresenceService presence;

    public record Heartbeat(Boolean active) {}

    @Operation(summary = "Lấy trạng thái hoạt động trong chat trực tiếp",
            description = "Trả trạng thái theo lô cho tối đa 100 cuộc hội thoại trực tiếp thường mà người gọi được phép xem.")
    @ApiResponse(responseCode = "200", description = "Trạng thái hoạt động của người đối thoại")
    @GetMapping("/conversations/presence")
    public GlobalResponse<List<ChatPresenceDto>> snapshot(
            @Parameter(description = "ID hội thoại, phân cách bằng dấu phẩy", example = "42,75")
            @RequestParam("conversation_ids") @Size(min = 1, max = 100) List<Long> conversationIds) {
        return GlobalResponse.success(presence.snapshot(JwtPayload.getCurrentUserPayload().getUserId(), conversationIds));
    }

    @MessageMapping("/chat.presence")
    public void heartbeat(@Payload Heartbeat request, @Header(SimpMessageHeaderAccessor.SESSION_ID_HEADER) String socketId,
            Principal principal) {
        if (request.active() != null) presence.heartbeat(JwtPayload.getUserId(principal), socketId, request.active());
    }
}
