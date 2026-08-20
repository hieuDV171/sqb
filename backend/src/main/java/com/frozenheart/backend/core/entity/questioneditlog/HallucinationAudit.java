package com.frozenheart.backend.core.entity.questioneditlog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HallucinationAudit {
    private double confidenceScore;
    private boolean hallucinated;
}
