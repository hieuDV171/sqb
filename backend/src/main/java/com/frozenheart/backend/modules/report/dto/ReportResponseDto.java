package com.frozenheart.backend.modules.report.dto;

import com.frozenheart.backend.core.entity.socialinteraction.ReportStatus;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Schema(description = "Kết quả ghi nhận đơn báo cáo vi phạm")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {

    @Schema(description = "ID bản ghi đơn báo cáo", example = "55")
    private Long reportId;

    @Schema(description = "Loại đối tượng bị báo cáo", example = "POST")
    private ReportTargetType targetType;

    @Schema(description = "ID đối tượng bị báo cáo", example = "105")
    private Long targetId;

    @Schema(description = "Trạng thái xử lý báo cáo (PENDING, RESOLVED)", example = "PENDING")
    private ReportStatus status;

    @Schema(description = "Thời điểm gửi báo cáo", example = "2026-10-06T12:00:00Z")
    private Instant createdAt;
}
