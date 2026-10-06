package com.frozenheart.backend.modules.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;

@Schema(description = "Yêu cầu gán sinh viên vào lớp học phần")
public record AssignStudentsRequest(
        @Schema(description = "Danh sách mã số sinh viên (MSSV)", example = "[\"20235233\", \"20235234\"]")
        List<String> studentCodes,

        @Schema(description = "Danh sách ID tài khoản người dùng trực tiếp", example = "[5, 6, 7]")
        List<Long> studentUserIds
) {}
