package com.frozenheart.backend.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

@Schema(description = "Danh sách URL media cần kiểm tra tồn tại")
public record MediaVerifyRequest(
        @Schema(description = "Các URL media cần kiểm tra tồn tại trong kho lưu trữ", example = "[\"https://media.example.com/image.png\"]")
        @NotEmpty(message = "Danh sách URL không được để trống")
    List<String> urls
) {}
