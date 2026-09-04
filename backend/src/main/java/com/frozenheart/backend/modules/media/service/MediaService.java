package com.frozenheart.backend.modules.media.service;

import org.springframework.web.multipart.MultipartFile;

import com.frozenheart.backend.core.entity.media.MediaPurpose;
import com.frozenheart.backend.modules.media.dto.MediaUploadResponse;
import com.frozenheart.backend.modules.media.dto.PresignMediaRequest;
import com.frozenheart.backend.modules.media.dto.PresignMediaResponse;

import java.util.List;

public interface MediaService {

    MediaUploadResponse uploadMedia(MultipartFile file, MediaPurpose purpose);

    void confirmMediaPermanent(List<String> objectKeys);

    PresignMediaResponse generatePresignedUrls(PresignMediaRequest request);

}
