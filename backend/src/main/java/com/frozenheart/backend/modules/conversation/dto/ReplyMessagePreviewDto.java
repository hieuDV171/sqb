package com.frozenheart.backend.modules.conversation.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReplyMessagePreviewDto {

    private Long id;

    private Long senderId;

    private String senderName;

    private String contentPreview;
}
