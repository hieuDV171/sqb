// Response DTO trả về cho /medias/presign
package com.frozenheart.backend.modules.media.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "Phản hồi danh sách URL ký trước để tải tệp lên Cloudflare R2")
public record PresignMediaResponse(

        @Schema(description = "Danh sách URL ký trước tương ứng với từng tệp yêu cầu")
        List<PresignedUrlItem> presignedUrls
) {
        
    @Builder
    @Schema(description = "Thông tin chi tiết đường dẫn tải lên cho một tệp")
    public record PresignedUrlItem(
            @Schema(description = "Mã khóa đối tượng duy nhất trên R2 S3", example = "users/169/avatars/uuid_filename.webp")
            String objectKey,

            @Schema(description = "URL ký trước phương thức PUT để Client upload trực tiếp lên R2", example = "https://r2.cloudflarestorage.com/sqb/...")
            String uploadUrl,

            @Schema(description = "URL công khai của tệp sau khi upload thành công", example = "https://media.sqb.edu.vn/users/169/avatars/uuid_filename.webp")
            String publicUrl,

            @Schema(description = "Thời gian hết hạn của link ký tính bằng giây", example = "900")
            int expiresInSeconds
    ) {
    }
    
}
