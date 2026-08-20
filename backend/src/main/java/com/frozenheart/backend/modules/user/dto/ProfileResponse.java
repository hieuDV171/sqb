package com.frozenheart.backend.modules.user.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.entity.user.UserRole;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        Long id,
        String email,
        String fullName,
        String avatarUrl,
        String coverUrl,
        String frameUrl,
        String bio,
        String faculty,
        String major,
        String studentLecturerCode,
        UserRole role,
        boolean profileCompleted,
        boolean verified,
                
        // Counters & Gamification
        int totalProposedQuestions,
        int totalApprovedQuestions,
        double gamificationPoints,
        int badgesCount,
        int friendsCount,
        int followersCount,
        int followingCount,
        
        // Feature Locking
        List<LockedFeature> lockedFeatures,

        LocalDateTime createdAt,

        Relationship relationships
    ) {

}
