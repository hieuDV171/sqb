package com.frozenheart.backend.modules.feedback.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record FeedbackRequest(
        @NotBlank @Size(max = 2000) String message,
        @Size(max = 512) @Pattern(regexp = "^/(?!/)[^?#\\p{Cntrl}]*\\z") String pagePath
) {
}
