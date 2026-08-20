package com.frozenheart.backend.core.entity.questioneditlog;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AiMetatdata {
    private String model;
    private int tokenUsed;
    private long timeProcessing;
}
