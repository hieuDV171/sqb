package com.frozenheart.backend.modules.exam.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Schema(description = "Kết quả xuất tài liệu (PDF / Excel)")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExportResponse {

    @Schema(description = "Đường dẫn tải file trực tiếp (Presigned URL)", example = "https://sqb.s3.ap-southeast-1.amazonaws.com/exports/exam_5_ans_false.pdf?X-Amz-Signature=...")
    private String downloadUrl;

    @Schema(description = "Dung lượng file tính bằng MB", example = "1.25")
    private Double fileSizeMb;

    @Schema(description = "Tổng số lượng câu hỏi có trong file xuất", example = "40")
    private Integer questionsCount;

    @Schema(description = "Thời điểm liên kết tải hết hạn", example = "2026-10-06T12:00:00Z")
    private Instant expiresAt;
}

