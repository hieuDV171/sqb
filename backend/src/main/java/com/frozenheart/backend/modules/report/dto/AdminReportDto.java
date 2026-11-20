package com.frozenheart.backend.modules.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.frozenheart.backend.core.entity.socialinteraction.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public final class AdminReportDto {
    private AdminReportDto() {}

    public enum TargetState { AVAILABLE, HIDDEN, SUSPENDED, UNAVAILABLE }
    public enum Decision { DISMISS, HIDE_CONTENT, WARN_ACCOUNT, SUSPEND_ACCOUNT }

    @Schema(description = "Tóm tắt báo cáo và trạng thái hiện tại của đối tượng bị báo cáo")
    public record Summary(
            @Schema(description = "ID báo cáo vi phạm", example = "120")
            Long id,
            @Schema(description = "Loại đối tượng được tham chiếu; dùng giá trị enum API công bố", example = "POST")
            String targetType,
            @Schema(description = "ID đối tượng được tham chiếu; diễn giải cùng target_type/type của API", example = "42")
            Long targetId,
            @Schema(description = "Loại vi phạm người dùng chọn theo enum API")
            ViolationType violationType,
            @Schema(description = "Trạng thái nghiệp vụ hiện tại theo enum của schema")
            ReportStatus status,
            @Schema(description = "Kết quả xử lý báo cáo, null nếu chưa được đóng")
            ReportOutcome outcome,
            @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
            Instant createdAt,
            @Schema(description = "Trích đoạn nội dung báo cáo, tối đa 160 code point trước dấu rút gọn", example = "Nội dung bị báo cáo cần được xem xét.")
            String preview,
            @Schema(description = "Tình trạng hiện tại của đối tượng bị báo cáo")
            TargetState targetState,
            @Schema(description = "Số báo cáo PENDING cùng đối tượng", example = "3")
            long pendingCount) {}

    @Schema(description = "Thông tin rút gọn người tham gia xử lý báo cáo")
    public record Person(
            @Schema(description = "ID tài khoản người gửi/người duyệt/quản trị viên", example = "7")
            Long id,
            @Schema(description = "Tên hiển thị của tài khoản", example = "Nguyễn Minh Anh")
            String fullName) {}

    @Schema(description = "Một biện pháp kiểm duyệt trong lịch sử, gồm quản trị viên và liên kết hoàn tác nếu có")
    public record Action(
            @Schema(description = "ID biện pháp kiểm duyệt, dùng khi hoàn tác", example = "42")
            Long id,
            @Schema(description = "Loại đối tượng theo enum của schema")
            ModerationActionType type,
            @Schema(description = "Lý do thao tác hoặc lý do thất bại của mục tương ứng", example = "Nội dung không phù hợp quy định cộng đồng")
            String reason,
            @Schema(description = "Thời điểm tạo dữ liệu, ISO-8601 UTC", example = "2026-11-19T07:00:00Z")
            Instant createdAt,
            @Schema(description = "Thời điểm hết đình chỉ, null nếu không áp dụng", example = "2026-11-19T07:00:00Z")
            Instant suspendedUntil,
            @Schema(description = "ID biện pháp gốc được hoàn tác; null nếu đây không phải hành động hoàn tác", example = "42")
            Long reversedActionId,
            @Schema(description = "ID báo cáo liên quan đến biện pháp", example = "42")
            Long reportId,
            @Schema(description = "Quản trị viên thực hiện biện pháp")
            Person admin) {}

    @Schema(description = "Chi tiết báo cáo, snapshot, version, hành động hợp lệ và lịch sử kiểm duyệt")
    public record Detail(
            @Schema(description = "Thông tin tóm tắt của báo cáo đang xem")
            Summary report,
            @Schema(description = "Phiên bản hiện tại để kiểm soát xử lý đồng thời; gửi đúng version từ GET detail", example = "0")
            Long version,
            @Schema(description = "Mô tả bổ sung do người gửi báo cáo cung cấp", example = "Nội dung có dấu hiệu quảng cáo không liên quan.")
            String description,
            @Schema(description = "Người gửi báo cáo, có thể null nếu tài khoản không còn tồn tại")
            Person reporter,
            @Schema(description = "Quản trị viên đã xử lý, null nếu chưa có")
            Person reviewer,
            @Schema(description = "Thời điểm hoàn tất xem xét, null nếu chưa duyệt", example = "2026-11-19T07:00:00Z")
            Instant reviewedAt,
            @Schema(description = "Lý do/kết luận do quản trị viên ghi khi xử lý", example = "Đã kiểm tra và xử lý theo quy định.")
            String resolutionNote,
            @Schema(description = "Bản chụp tại thời điểm gửi báo cáo, có thể thiếu ở dữ liệu legacy")
            ReportSnapshot snapshot,
            @Schema(description = "Snapshot nội dung hiện tại; có thể null nếu đối tượng không còn khả dụng")
            ReportSnapshot current,
            @Schema(description = "Thời điểm hết đình chỉ, null nếu không áp dụng", example = "2026-11-19T07:00:00Z")
            Instant suspendedUntil,
            @Schema(description = "ID biện pháp đang có hiệu lực, dùng khi yêu cầu hoàn tác", example = "42")
            Long activeActionId,
            @Schema(description = "Các quyết định resolve hiện được phép theo loại và trạng thái đối tượng")
            List<Decision> allowedActions,
            @Schema(description = "Lịch sử các biện pháp kiểm duyệt liên quan")
            List<Action> history,
            @Schema(description = "Báo cáo có bằng chứng riêng tư để xin URL xem", example = "true")
            boolean hasEvidence) {}

    @Schema(description = "Quyết định đóng báo cáo PENDING; gửi version hiện tại để phát hiện xử lý đồng thời")
    public record ResolveRequest(
            @Schema(description = "Phiên bản hiện tại để kiểm soát xử lý đồng thời; gửi đúng version từ GET detail", example = "0")
            @NotNull @PositiveOrZero Long version,
            @Schema(description = "Quyết định kiểm duyệt; chỉ chọn trong allowed_actions của báo cáo", example = "SUSPEND_ACCOUNT")
            @NotNull Decision action,
            @Schema(description = "Lý do thao tác hoặc lý do thất bại của mục tương ứng", example = "Nội dung không phù hợp quy định cộng đồng")
            @NotBlank @Size(max = 500) String reason,
            @Schema(description = "Chỉ gửi khi SUSPEND_ACCOUNT, nhận 1/7/30 ngày; các action khác phải bỏ trống", example = "7")
            Integer suspensionDays) {}

    @Schema(description = "Lý do hoàn tác một biện pháp kiểm duyệt đang có hiệu lực")
    public record ReverseRequest(
            @Schema(description = "Lý do thao tác hoặc lý do thất bại của mục tương ứng", example = "Nội dung không phù hợp quy định cộng đồng")
            @NotBlank @Size(max = 500) String reason) {}

    @Schema(description = "URL GET có chữ ký 120 giây xem bằng chứng riêng tư; không dùng làm URL lưu bền vững")
    public record Evidence(
            @Schema(description = "URL GET riêng tư có chữ ký, hết hạn sau 120 giây; gọi lại API evidence để lấy URL mới", example = "https://r2.example.com/report-evidence/image?signature=example")
            String url) {}
}
