package com.frozenheart.backend.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRefineRequest(
        @NotBlank(message = "Prompt không được để trống")
        String prompt,
        String chatSessionId
) {}
