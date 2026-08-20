package com.frozenheart.backend.core.entity.questioneditlog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HallucinationCheck {
    private boolean isHallucinated;
    private double confidenceScore;
}
