package com.frozenheart.backend.modules.media.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.media.MediaPurpose;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record PresignMediaRequest(

    @NotEmpty(message = "Danh sách tệp không được để trống")
    @Size(max = 10, message = "Chỉ được yêu cầu tải lên tối đa 10 tệp cùng lúc")
    @Valid
    List<PresignFileItem> files
    
) {
    public record PresignFileItem(

        @NotBlank(message = "Tên tệp không được để trống")
        String fileName,
                
        @NotNull(message = "Dung lượng tệp không được để trống")
        @Positive(message = "Dung lượng tệp phải lớn hơn 0")
        @Max(value = 15 * 1024 * 1024L, message = "Dung lượng tệp không được vượt quá 15MB")
        Long fileSize,
                
        @NotBlank(message = "Định dạng tệp không được để trống")
        String contentType,
                
        @NotNull(message = "Mục đích upload không được để trống")
        MediaPurpose purpose
        
    ) {}
}
