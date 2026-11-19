package com.frozenheart.backend.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Các URL trong yêu cầu không tìm thấy trong kho lưu trữ")
public record MediaVerifyResponse(
        @Schema(description = "Các URL được yêu cầu nhưng không tìm thấy", example = "[\"https://media.example.com/missing.png\"]")
        List<String> missingUrls
) {}
