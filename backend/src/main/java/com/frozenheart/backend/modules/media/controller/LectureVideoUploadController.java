package com.frozenheart.backend.modules.media.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.modules.media.dto.LectureVideoUpload;
import com.frozenheart.backend.modules.media.service.LectureVideoUploadService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/lecturer/medias/lecture-uploads")
@RequiredArgsConstructor
@Tag(name = "Tải video bài giảng")
public class LectureVideoUploadController {
    private final LectureVideoUploadService service;

    @PutMapping("/{id}")
    public GlobalResponse<LectureVideoUpload.Status> start(
            @PathVariable UUID id, @Valid @RequestBody LectureVideoUpload.StartRequest request) {
        return GlobalResponse.success(service.start(id, request));
    }

    @GetMapping("/{id}")
    public GlobalResponse<LectureVideoUpload.Status> status(@PathVariable UUID id) {
        return GlobalResponse.success(service.status(id));
    }

    @PostMapping("/{id}/parts/{number}")
    public GlobalResponse<LectureVideoUpload.PartUrl> partUrl(@PathVariable UUID id, @PathVariable int number) {
        return GlobalResponse.success(service.partUrl(id, number));
    }

    @PostMapping("/{id}/complete")
    public GlobalResponse<MediaItem> complete(@PathVariable UUID id) {
        return GlobalResponse.success(service.complete(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        service.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
