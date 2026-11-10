package com.frozenheart.backend.modules.conversation.dto;

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
public class MessageListResponseDto {

    private Long conversationId;

    private ConversationType conversationType;

    private Instant hiddenAt;

    private List<MessageDto> messages;

    private CursorPaginationDto pagination;
}
