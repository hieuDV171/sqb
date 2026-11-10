package com.frozenheart.backend.modules.conversation.dto;

public record GroupCandidateDto(Long userId, String fullName, String avatarUrl,
        boolean friend, boolean following, boolean follower) {
}
