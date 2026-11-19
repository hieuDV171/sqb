package com.frozenheart.backend.modules.badge.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Danh sách tài khoản cần thu hồi huy hiệu được chọn")
public record RevokeBadgeRequest(
        @Schema(description = "Danh sách ID tài khoản cần xử lý; không phải ID huy hiệu", example = "[7,8]")
        @NotEmpty @Size(max = 100) List<@NotNull @Positive Long> userIds) {
}
