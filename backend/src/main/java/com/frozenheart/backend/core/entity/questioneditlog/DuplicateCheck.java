package com.frozenheart.backend.core.entity.questioneditlog;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class DuplicateCheck {
    private boolean isDuplicate;
    private double similarityScore;
    private List<Integer> similarMediaIds;
}
