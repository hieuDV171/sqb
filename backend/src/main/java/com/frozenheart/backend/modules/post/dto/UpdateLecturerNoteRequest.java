package com.frozenheart.backend.modules.post.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Yêu cầu thêm hoặc cập nhật ghi chú chuyên môn của Giảng viên cho bài viết")
public class UpdateLecturerNoteRequest {
    @NotBlank(message = "Nội dung không thể trống")
    @Schema(description = "Nội dung ghi chú hoặc lời khuyên chuyên môn của giảng viên", example = "Lời giải rất tốt, tuy nhiên ở bước 2 cần chú ý điều kiện biên khi mảng rỗng.")
    private String content;
}
