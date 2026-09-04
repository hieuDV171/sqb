package com.frozenheart.backend.modules.report.dto;

import com.frozenheart.backend.core.entity.socialinteraction.ViolationType;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequestDto {

    @NotNull(message = "violation_type không được để trống")
    private ViolationType violationType;

    @Size(max = 500, message = "description tối đa 500 ký tự")
    private String description;

    private List<String> evidenceUrls;

    @NotNull(message = "target_type không được để trống")
    private ReportTargetType targetType;

    @NotNull(message = "target_id không được để trống")
    private Long targetId;
}
