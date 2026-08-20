package com.frozenheart.backend.core.entity.session;

import lombok.Builder;

@Builder
public record DuplicateWarning(
        int questionIndex,
        Long similarQuestionId,
        double similarityScore,
        DuplicateDetectionTier tier,
        String matchedSnipet,
        String mediaUrl
    ) {

}
