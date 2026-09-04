package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import com.frozenheart.backend.core.entity.conversation.MessageType;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsMessageBroadcastDto {

    @Builder.Default
    private ChatWsEventType eventType = ChatWsEventType.NEW_MESSAGE;

    private Long messageId;

    private Long conversationId;

    private ConversationType conversationType;

    private UserSummaryDto sender;

    private String content;

    private MessageType messageType;

    private List<String> mediaUrls;

    private Long replyToMessageId;

    private LocalDateTime createdAt;

    private boolean isEdited;
}
