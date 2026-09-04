// Response DTO trả về cho /medias/presign
package com.frozenheart.backend.modules.media.dto;

import lombok.Builder;
import java.util.List;

@Builder
public record PresignMediaResponse(

        List<PresignedUrlItem> presignedUrls
) {
        
    @Builder
    public record PresignedUrlItem(
            String objectKey,
            String uploadUrl,
            String publicUrl,
            int expiresInSeconds
    ) {
    }
    
}
