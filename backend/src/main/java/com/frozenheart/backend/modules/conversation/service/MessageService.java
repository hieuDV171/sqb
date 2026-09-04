package com.frozenheart.backend.modules.conversation.service;

import com.frozenheart.backend.modules.conversation.dto.DeleteMessageRequestDto;
import com.frozenheart.backend.modules.conversation.dto.DeleteMessageResponseDto;
import com.frozenheart.backend.modules.conversation.dto.MessageListResponseDto;

public interface MessageService {

    MessageListResponseDto getMessages(Long conversationId, Long after, Integer limit);

    DeleteMessageResponseDto deleteMessage(Long messageId, DeleteMessageRequestDto request);
}
