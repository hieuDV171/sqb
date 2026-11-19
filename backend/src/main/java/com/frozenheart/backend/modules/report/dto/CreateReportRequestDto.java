package com.frozenheart.backend.modules.report.dto;

import com.frozenheart.backend.core.entity.socialinteraction.ViolationType;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Schema(description = "Yêu cầu gửi báo cáo vi phạm nội dung hoặc hành vi người dùng")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateReportRequestDto {

    @Schema(description = "Loại vi phạm nội dung (SPAM, HARASSMENT, HATE_SPEECH, INAPPROPRIATE_CONTENT, COPYRIGHT, OTHER)", example = "SPAM")
    @NotNull(message = "violation_type không được để trống")
    private ViolationType violationType;

    @Schema(description = "Mô tả chi tiết nội dung vi phạm", example = "Bài viết này liên tục quảng cáo link rác không liên quan")
    @Size(max = 500, message = "description tối đa 500 ký tự")
    private String description;

    @Schema(description = "Danh sách URL hình ảnh bằng chứng (nếu có)", example = "[\"https://sqb.s3.../evidence1.png\"]")
    @Size(max = 1)
    private List<@NotBlank @Size(max = 2048) String> evidenceUrls;

    @Schema(description = "Tối đa một object key từ POST /report/evidence của chính người gửi; không gửi đồng thời evidence_urls", example = "[\"report-evidence/7/550e8400-e29b-41d4-a716-446655440000\"]")
    @Size(max = 1)
    private List<@NotBlank @Size(max = 160) String> evidenceKeys;

    @Schema(description = "Loại đối tượng bị báo cáo (USER, POST, COMMENT)", example = "POST")
    @NotNull(message = "target_type không được để trống")
    private ReportTargetType targetType;

    @Schema(description = "ID của đối tượng bị báo cáo", example = "105")
    @NotNull(message = "target_id không được để trống")
    @Positive
    private Long targetId;
}
