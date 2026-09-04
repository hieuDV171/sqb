package com.frozenheart.backend.core.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public enum Point {
    APPROVED_QUESTION(1.0),
    GAME1_AWARD(0.5),
    GAME2_AWARD(0.5),
    GAME3_AWARD(0.25),
    GAME4_AWARD(1.0),
    GAME5_REPORT_AWARD(0.5),
    GAME5_AUTHOR_PENALTY(1.0),
    GAME6_AWARD(1.0);

    private double points;
}
