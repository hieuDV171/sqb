package com.frozenheart.backend.modules.media.dto;

import java.util.List;

import com.frozenheart.backend.core.entity.media.MediaPurpose;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PresignMediaRequest(

    @NotEmpty(message = "Danh sách file không được để trống")
    @Valid
    List<PresignFileItem> files
    
) {
    public record PresignFileItem(

        @NotBlank(message = "Tên file không được để trống")
        String fileName,
                
        @NotNull(message = "Dung lượng file không được để trống")
        Long fileSize,
                
        @NotBlank(message = "Type file không được để trống")
        String contentType,
                
        @NotNull(message = "Mục đích upload không được để trống")
        MediaPurpose purpose
        
    ) {}
}
