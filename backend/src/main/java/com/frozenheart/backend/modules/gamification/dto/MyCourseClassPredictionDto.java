package com.frozenheart.backend.modules.gamification.dto;

import lombok.Builder;

@Builder
public record MyCourseClassPredictionDto(
        Long courseClassId,
        String classCode,
        String semester,
        Long subjectId,
        String subjectName,
        String lecturerName,
        boolean alreadyPredicted,
        Integer predictedCount
) {}
