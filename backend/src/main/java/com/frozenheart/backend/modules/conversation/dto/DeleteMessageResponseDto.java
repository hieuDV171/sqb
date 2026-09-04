package com.frozenheart.backend.modules.conversation.dto;

import com.frozenheart.backend.modules.conversation.constant.MessageDeleteScope;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteMessageResponseDto {

    private Long messageId;

    private MessageDeleteScope scope;

    private String note;
}
