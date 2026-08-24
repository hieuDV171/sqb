package com.frozenheart.backend.core.entity.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GamificationPointsJson implements Serializable {

    @Builder.Default
    private double publicPoints = 0.0;

    @Builder.Default
    private double secretPoints = 0.0;

}
