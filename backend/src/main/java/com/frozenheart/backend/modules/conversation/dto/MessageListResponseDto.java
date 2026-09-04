package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.conversation.ConversationType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageListResponseDto {

    private Long conversationId;

    private ConversationType conversationType;

    private List<MessageDto> messages;

    private CursorPaginationDto pagination;
}
