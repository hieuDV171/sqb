package com.frozenheart.backend.modules.gamification.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewErrorRequest(
        @NotNull(message = "Thiếu xác nhận của giảng viên")
        Boolean isComfirmed,
        String notes
) {}
