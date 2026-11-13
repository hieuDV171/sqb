package com.frozenheart.backend.modules.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Schema(description = "Yêu cầu cấp phát huy hiệu thủ công cho người dùng (Dành cho Quản trị viên)")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ManualGrantBadgeRequestDto {

    @Schema(description = "Danh sách ID người dùng được cấp huy hiệu", example = "[10, 15, 23]")
    @NotEmpty(message = "Danh sách user_ids không được để trống")
    @Size(max = 100)
    private List<@NotNull @Positive Long> userIds;

    @Schema(description = "Lý do trao thưởng hoặc ghi chú của Quản trị viên", example = "Đạt giải nhất cuộc thi lập trình tuần 42")
    @Size(max = 255)
    private String reason;
}
