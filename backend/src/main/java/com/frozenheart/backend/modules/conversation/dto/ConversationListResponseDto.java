package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationListResponseDto {

    private List<ConversationDto> items;

    private CursorPaginationDto pagination;

    private int totalConversations;
}
