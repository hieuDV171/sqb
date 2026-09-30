package com.frozenheart.backend.modules.media.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

public record MediaVerifyRequest(
    @NotEmpty(message = "Danh sách URL không được để trống")
    List<String> urls
) {}
