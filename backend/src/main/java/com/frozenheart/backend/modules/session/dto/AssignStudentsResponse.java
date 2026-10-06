package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import java.util.List;

@Builder
@Schema(description = "Kết quả gán sinh viên vào lớp học phần")
public record AssignStudentsResponse(
        @Schema(description = "Số lượng sinh viên mới được ghi danh thành công vào lớp", example = "45")
        int totalEnrolled,

        @Schema(description = "Số lượng sinh viên đã có trong lớp từ trước", example = "5")
        int alreadyEnrolledCount,

        @Schema(description = "Số lượng mã sinh viên không tìm thấy trong hệ thống", example = "2")
        int notFoundCount,

        @Schema(description = "Danh sách các mã sinh viên không tồn tại trong hệ thống", example = "[\"20239999\", \"20238888\"]")
        List<String> notFoundCodes
) {}
