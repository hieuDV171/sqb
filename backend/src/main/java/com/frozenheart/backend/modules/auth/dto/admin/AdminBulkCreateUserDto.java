package com.frozenheart.backend.modules.auth.dto.admin;

import java.time.LocalDate;
import java.util.List;

import com.frozenheart.backend.core.entity.user.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

public class AdminBulkCreateUserDto {
    @Schema(description = "Thông tin chi tiết một tài khoản cần tạo mới/import")
    public record SingleUserImportDto(
            @Schema(description = "Email tài khoản", example = "an.nv20211234@sis.hust.edu.vn", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
            @Email(message = "INVALID_PARAMETER_VALUE") 
            String email,

            @Schema(description = "Mật khẩu ban đầu (bỏ trống để tự sinh theo ngày sinh ddMMyyyy hoặc mã số)", example = "123456789")
            String password,

            @Schema(description = "Họ và tên đầy đủ", example = "Nguyễn Văn An", requiredMode = Schema.RequiredMode.REQUIRED)
            @NotBlank(message = "MISSING_REQUIRED_PARAMETER") 
            String fullName,

            @Schema(description = "Mã số sinh viên (MSSV) hoặc Mã số giảng viên (MSGV)", example = "20211234")
            String studentLecturerCode,
            
            @Schema(description = "Trường / Khoa / Viện đào tạo", example = "Trường Công nghệ Thông tin và Truyền thông")
            String schoolFaculty,

            @Schema(description = "Chuyên ngành đào tạo", example = "Khoa học Máy tính")
            String major,

            @Schema(description = "Tên lớp sinh hoạt", example = "Khoa học máy tính 01-K69")
            String className,

            @Schema(description = "Giới tính (chỉ nhận MALE hoặc FEMALE)", allowableValues = {"MALE", "FEMALE"}, example = "MALE")
            Gender gender,

            @Schema(description = "Ngày tháng năm sinh (định dạng YYYY-MM-DD)", example = "2003-05-15")
            @Past(message = "Ngày sinh phải là ngày trong quá khứ")
            LocalDate dateOfBirth,

            @Schema(description = "Vai trò người dùng trong hệ thống (STUDENT, LECTURER, ADMIN)", example = "STUDENT")
            UserRoleDto role,

            @Schema(description = "Múi giờ hoạt động", example = "Asia/Ho_Chi_Minh")
            String timezone
        ) {
    }

    @Schema(description = "Yêu cầu tạo hàng loạt tài khoản người dùng")
    public record BulkImportRequest(
        @Schema(description = "Danh sách người dùng cần tạo")
        List<SingleUserImportDto> users
    ) {
    }

    @Schema(description = "Kết quả xử lý tạo hàng loạt người dùng")
    public record BulkImportResult(
        @Schema(description = "Tổng số tài khoản tạo thành công", example = "45")
        int totalSuccess,

        @Schema(description = "Tổng số tài khoản thất bại hoặc bị trùng lặp", example = "2")
        int totalFailed,

        @Schema(description = "Danh sách chi tiết các thông báo lỗi nếu có", example = "[\"Email hieu.dv224980@sis.hust.edu.vn đã tồn tại\"]")
        List<String> errors
    ) {
    }
}
