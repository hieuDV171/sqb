package com.frozenheart.backend.modules.ai.dto;

import lombok.Builder;

import java.util.List;

@Builder
public record BtpropSidecarRequest(
        String subject,
        String topic,
        String content,
        List<OptionDto> options,
        String explanation,
        boolean fastMode
) {
    @Builder
    public record OptionDto(
            String key,
            String text,
            boolean isCorrect,
            String mediaUrl,
            Long mediaId
    ) {}
}
