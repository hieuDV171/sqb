package com.frozenheart.backend.modules.ai.dto;

import jakarta.validation.constraints.NotNull;

public record AiApplyRequest(
        @NotNull(message = "editLogId không được để trống")
        Long editLogId,
        @NotNull(message = "approve không được để trống")
        Boolean approve
) {}
