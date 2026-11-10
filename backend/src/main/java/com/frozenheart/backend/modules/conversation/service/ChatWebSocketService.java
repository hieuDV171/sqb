package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.modules.conversation.dto.ws.ReadWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.SendWsMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.ws.TypingWsRequestDto;

public interface ChatWebSocketService {

    void handleSendMessage(JwtPayload session, SendWsMessageRequestDto request);

    void handleReadMessage(JwtPayload session, ReadWsMessageRequestDto request);

    void handleTyping(JwtPayload session, TypingWsRequestDto request);
}
