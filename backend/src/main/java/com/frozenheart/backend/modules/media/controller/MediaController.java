package com.frozenheart.backend.modules.media.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.media.MediaPurpose;
import com.frozenheart.backend.modules.media.dto.MediaUploadResponse;
import com.frozenheart.backend.modules.media.dto.MediaVerifyRequest;
import com.frozenheart.backend.modules.media.dto.MediaVerifyResponse;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest;
import com.frozenheart.backend.modules.media.dto.PresignMediaResponse;
import com.frozenheart.backend.modules.media.service.MediaService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/medias")
@RequiredArgsConstructor
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GlobalResponse<MediaUploadResponse>> uploadMedia(
        @RequestPart("file") MultipartFile file,
        @RequestParam("purpose") MediaPurpose purpose
    ) {

        MediaUploadResponse response = mediaService.uploadMedia(file, purpose);

        return ResponseEntity.ok(GlobalResponse.success(response));

    }

    @PostMapping("/presign")
    public ResponseEntity<GlobalResponse<PresignMediaResponse>> getPresignedUrls(
            @Valid @RequestBody PresignMediaRequest request
    ) {

        PresignMediaResponse response = mediaService.generatePresignedUrls(request);

        return ResponseEntity.ok(GlobalResponse.success(response));
        
    }

    @PostMapping("/verify")
    public ResponseEntity<GlobalResponse<MediaVerifyResponse>> verifyMedias(
            @Valid @RequestBody MediaVerifyRequest request
    ) {
        List<String> missingUrls = mediaService.findMissingObjects(request.urls());
        return ResponseEntity.ok(GlobalResponse.success(new MediaVerifyResponse(missingUrls)));
    }

}
