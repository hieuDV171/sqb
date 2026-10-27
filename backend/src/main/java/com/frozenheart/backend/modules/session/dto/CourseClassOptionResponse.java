package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Builder
@Schema(description = "Lớp học phần có thể hiển thị trong danh sách đề xuất câu hỏi")
public record CourseClassOptionResponse(
        Long courseClassId,
        String classCode,
        Long subjectId,
        String subjectCode,
        String subjectName,
        Long semesterId,
        String semesterName,
        boolean activeSemester,
        boolean canPropose
) {
}
