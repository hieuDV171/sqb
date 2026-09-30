package com.frozenheart.backend.modules.user.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.frozenheart.backend.core.entity.user.Gender;
import com.frozenheart.backend.core.entity.user.UserRole;

import lombok.Builder;

@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProfileResponse(
        Long userId,
        String email,
        String fullName,
        String avatarUrl,
        String coverUrl,
        String frameUrl,
        Gender gender,
        LocalDate dateOfBirth,
        String bio,
        String schoolFaculty,
        String major,
        String className,
        String studentLecturerCode,
        UserRole role,
        String timezone,
        boolean profileCompleted,
        boolean verified,

        // Counters & Gamification
        int totalProposedQuestions,
        double gamificationPoints,
        int badgesCount,
        int friendsCount,
        int followersCount,
        int followingCount,

        Instant createdAt,

        Relationship relationships) {

}
