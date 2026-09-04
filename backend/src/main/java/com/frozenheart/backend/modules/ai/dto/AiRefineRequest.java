package com.frozenheart.backend.modules.ai.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record AiRefineRequest(
        @NotBlank(message = "Prompt không được để trống")
        @Size(max = 1000, message = "Prompt không được vượt quá 1000 ký tự")
        String prompt,
        String chatSessionId
) {}
