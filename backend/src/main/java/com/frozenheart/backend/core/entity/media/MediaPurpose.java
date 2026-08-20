package com.frozenheart.backend.core.entity.media;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MediaPurpose {
    AVATAR("avatars"),
    COVER("covers"),
    QUESTION("questions"),
    POST("posts"),
    COMMENT("comments"),
    CHAT("chat"),
    DOCUMENT("documents");

    private final String folderName;
    
}
