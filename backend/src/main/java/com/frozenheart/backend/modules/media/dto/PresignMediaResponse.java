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
            String objectKey, // e.g. "questions/2026/08/unique-id.jpg"
            String uploadUrl, // Link PUT cấp phép ngắm của MinIO/S3 (hiệu lực 15 phút)
            String publicUrl, // Link công khai để FE lưu vào DB
            int expiresInSeconds // e.g. 900
    ) {
    }
    
}
