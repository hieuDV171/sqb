package com.frozenheart.backend.modules.session.dto;

import java.time.Instant;

import com.frozenheart.backend.core.entity.session.SessionStatus;

import lombok.Builder;

@Builder
public record ProposeSessionResponse(
                Long sessionId,
                String sessionCode,
                Long subjectId,
                int questionCount,
                SessionStatus status,
                Instant createdAt) {

}
