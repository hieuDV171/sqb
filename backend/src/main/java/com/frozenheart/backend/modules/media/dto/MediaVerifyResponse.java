package com.frozenheart.backend.modules.media.dto;

import java.util.List;

public record MediaVerifyResponse(
    List<String> missingUrls
) {}
