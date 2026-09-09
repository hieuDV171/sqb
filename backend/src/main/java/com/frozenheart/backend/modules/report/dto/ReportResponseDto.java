package com.frozenheart.backend.modules.report.dto;

import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    private Long reportId;

    private ReportTargetType targetType;

    private Long targetId;

    private ReportStatus status;

    private Instant createdAt;
}
