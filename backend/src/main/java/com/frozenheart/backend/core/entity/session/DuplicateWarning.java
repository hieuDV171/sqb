package com.frozenheart.backend.core.entity.session;

import com.fasterxml.jackson.annotation.JsonAlias;
import lombok.Builder;

@Builder
public record DuplicateWarning(
        Long questionId,
        Integer questionIndex,
        Long similarQuestionId,
        Scope scope,
        Kind kind,
        Double similarityScore,
        Integer hammingDistance,
        DuplicateDetectionTier tier,
        @JsonAlias({"matchedSnipet", "matched_snipet"}) String matchedSnippet,
        String mediaUrl) {

    public enum Scope { SAME_SESSION, QUESTION_BANK }
    public enum Kind { EXACT, ANSWER_CONFLICT, SIMILAR }
}
