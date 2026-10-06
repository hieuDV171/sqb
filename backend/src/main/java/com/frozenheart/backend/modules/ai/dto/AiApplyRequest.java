package com.frozenheart.backend.modules.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu phê duyệt hoặc hủy bỏ kết quả chỉnh sửa câu hỏi của AI")
public record AiApplyRequest(
        @Schema(description = "ID nhật ký chỉnh sửa AI cần phê duyệt/hủy bỏ", example = "105")
        @NotNull(message = "editLogId không được để trống")
        Long editLogId,

        @Schema(description = "true: Áp dụng chỉnh sửa vào câu hỏi gốc, false: Loại bỏ", example = "true")
        @NotNull(message = "approve không được để trống")
        Boolean approve
) {}
