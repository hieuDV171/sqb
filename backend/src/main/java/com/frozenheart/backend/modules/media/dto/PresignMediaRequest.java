package com.frozenheart.backend.modules.media.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.media.MediaPurpose;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(description = "Yêu cầu cấp URL ký trước (Presigned URL) để tải media trực tiếp lên Cloudflare R2")
public record PresignMediaRequest(

    @Schema(description = "Danh sách các tệp tin cần tải lên (tối đa 10 tệp)")
    @NotEmpty(message = "Danh sách tệp không được để trống")
    @Size(max = 10, message = "Chỉ được yêu cầu tải lên tối đa 10 tệp cùng lúc")
    @Valid
    List<PresignFileItem> files
    
) {
    @Schema(description = "Thông tin một tệp tin cần xin presigned URL")
    public record PresignFileItem(

        @Schema(description = "Tên gốc của tệp tin", example = "de_thi_cuoi_ky.pdf")
        @NotBlank(message = "Tên tệp không được để trống")
        String fileName,
                
        @Schema(description = "Kích thước tệp tính bằng byte (tối đa 15MB)", example = "2048576")
        @NotNull(message = "Dung lượng tệp không được để trống")
        @Positive(message = "Dung lượng tệp phải lớn hơn 0")
        @Max(value = 15 * 1024 * 1024L, message = "Dung lượng tệp không được vượt quá 15MB")
        Long fileSize,
                
        @Schema(description = "MIME Content-Type của tệp", example = "image/webp")
        @NotBlank(message = "Định dạng tệp không được để trống")
        String contentType,
                
        @Schema(description = "Mục đích tải lên (AVATAR, COVER, POST, QUESTION, CHAT...)", example = "AVATAR")
        @NotNull(message = "Mục đích upload không được để trống")
        MediaPurpose purpose
        
    ) {}
}
