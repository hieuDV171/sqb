package com.frozenheart.backend.core.entity.questioneditlog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiAnalysisResult {
    private DuplicateCheck duplicateCheck;
    private HallucinationCheck hallucinationCheck;
}
