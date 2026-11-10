package com.frozenheart.backend.modules.conversation.dto;

import java.time.Instant;

public record GroupInvitationDto(Long invitationId, String groupName, String groupAvatarUrl,
        String inviterName, Instant expiresAt) {
}
