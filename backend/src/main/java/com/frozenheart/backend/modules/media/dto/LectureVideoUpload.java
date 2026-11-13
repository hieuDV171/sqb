package com.frozenheart.backend.modules.media.dto;

import com.frozenheart.backend.core.entity.media.MediaItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public final class LectureVideoUpload {
    private LectureVideoUpload() {}

    public record StartRequest(
            @NotBlank @Size(max = 255) String fileName,
            @NotBlank @Size(max = 100) String contentType,
            @Positive long fileSize
    ) {}

    public record PartUrl(String uploadUrl) {}

    public record Status(UUID id, long partSize, List<Integer> uploadedParts, MediaItem media) {}
}
