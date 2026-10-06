package com.frozenheart.backend.modules.gamification.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Yêu cầu giảng viên/quản trị viên thẩm định báo lỗi câu hỏi (Game 5)")
public record ReviewErrorRequest(
        @Schema(description = "Xác nhận câu hỏi có bị lỗi thực sự hay không (true: chấp nhận lỗi và thưởng điểm người báo, phạt tác giả; false: từ chối)", example = "true")
        @NotNull(message = "Thiếu xác nhận của giảng viên")
        Boolean isComfirmed,

        @Schema(description = "Ghi chú hoặc giải thích của người thẩm định", example = "Đã kiểm tra, câu hỏi thiếu dữ kiện ở phương án C.")
        String notes
) {}

