package com.frozenheart.backend.modules.conversation.dto.ws;

import com.frozenheart.backend.core.dto.user.UserSummaryDto;
import com.frozenheart.backend.modules.conversation.constant.ChatWsEventType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WsReadReceiptBroadcastDto {

    @Builder.Default
    private ChatWsEventType eventType = ChatWsEventType.MESSAGE_READ;

    private Long conversationId;

    private Long messageId;

    private UserSummaryDto reader;

    private LocalDateTime readAt;
}
