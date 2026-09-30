package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyMessagePreviewDto {

    private Long messageId;

    private Long senderId;

    private String senderName;

    private String contentPreview;
}
