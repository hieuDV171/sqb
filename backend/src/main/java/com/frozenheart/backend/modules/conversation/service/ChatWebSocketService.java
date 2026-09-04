package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.modules.conversation.dto.ws.ReadWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.SendWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.TypingWsRequestDto;

public interface ChatWebSocketService {

    void handleSendMessage(Long currentUserId, SendWsMessageRequestDto request);

    void handleReadMessage(Long currentUserId, ReadWsMessageRequestDto request);

    void handleTyping(Long currentUserId, TypingWsRequestDto request);
}
