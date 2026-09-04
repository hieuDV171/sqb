package com.frozenheart.backend.core.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationPointsJson implements Serializable {

    @Builder.Default
    private double publicPoints = 0.0;

    @Builder.Default
    private double secretPoints = 0.0;

    @Builder.Default
    private double coinBalance = 0.0;

    public GamificationPointsJson(double publicPoints, double secretPoints) {
        this.publicPoints = publicPoints;
        this.secretPoints = secretPoints;
        this.coinBalance = publicPoints;
    }
}
