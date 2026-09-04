package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.conversation.dto.ws.ReadWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.SendWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.TypingWsRequestDto;
import com.frozenheart.backend.modules.conversation.service.ChatWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Slf4j
@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatWebSocketService chatWebSocketService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload SendWsMessageRequestDto request, Principal principal) {
        Long userId = requireUserId(principal);
        chatWebSocketService.handleSendMessage(userId, request);
    }

    @MessageMapping("/chat.read")
    public void readMessage(@Payload ReadWsMessageRequestDto request, Principal principal) {
        Long userId = requireUserId(principal);
        chatWebSocketService.handleReadMessage(userId, request);
    }

    @MessageMapping("/chat.typing")
    public void typing(@Payload TypingWsRequestDto request, Principal principal) {
        Long userId = requireUserId(principal);
        chatWebSocketService.handleTyping(userId, request);
    }

    private Long requireUserId(Principal principal) {
        Long userId = JwtPayload.getUserId(principal);
        if (userId == null) {
            log.error("[ChatWebSocketController] Unauthorized WebSocket invocation rejected");
            throw new AppException(ResponseCode.ACCESS_DENIED, "Chưa xác thực người dùng");
        }
        return userId;
    }
}
