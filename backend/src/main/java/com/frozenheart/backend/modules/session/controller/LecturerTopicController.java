package com.frozenheart.backend.modules.session.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.session.service.TopicService;
import com.frozenheart.backend.modules.session.service.TopicService.ManagedTopicResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Tag(name = "15c. Quản lý chủ đề môn học",
        description = "Dành cho quản trị viên và giảng viên được phân công dạy môn")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/lecturer/subjects/{subjectId}/topics")
@RequiredArgsConstructor
public class LecturerTopicController {
    private final TopicService topics;

    @Schema(description = "Yêu cầu tạo chủ đề thuộc một môn học")
    public record CreateTopicRequest(
            @Schema(description = "Tên duy nhất trong môn học khi đối chiếu không phân biệt hoa thường, tối đa 100 ký tự", example = "Định thời CPU")
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "Trạng thái sử dụng, mặc định là true khi bỏ qua", example = "true")
            Boolean active) {}

    @Schema(description = "Yêu cầu cập nhật chủ đề dựa trên phiên bản đã đọc")
    public record UpdateTopicRequest(
            @Schema(description = "Tên duy nhất trong môn học khi đối chiếu không phân biệt hoa thường, tối đa 100 ký tự", example = "Định thời CPU")
            @NotBlank @Size(max = 100) String name,
            @Schema(description = "Trạng thái sử dụng. Bỏ qua để giữ trạng thái hiện tại.", example = "false")
            Boolean active,
            @Schema(description = "Phiên bản chủ đề nhận được khi đọc danh mục quản lý hoặc lưu lần trước", example = "2")
            @NotNull @PositiveOrZero Long version) {}

    @Operation(summary = "Liệt kê chủ đề để quản lý",
            description = "Trả về cả chủ đề đang dùng và đã ngừng sử dụng theo tên, kèm phiên bản của từng chủ đề.")
    @GetMapping
    public GlobalResponse<List<ManagedTopicResponse>> list(
            @Parameter(description = "ID môn học cần quản lý chủ đề", example = "12") @PathVariable Long subjectId) {
        return GlobalResponse.success(topics.listManaged(subjectId));
    }

    @Operation(summary = "Tạo chủ đề cho môn học",
            description = "Tạo chủ đề và trả về phiên bản ban đầu. Chủ đề mới ở trạng thái đang dùng nếu bỏ qua active.")
    @PostMapping
    public GlobalResponse<ManagedTopicResponse> create(
            @Parameter(description = "ID môn học sẽ chứa chủ đề mới", example = "12") @PathVariable Long subjectId,
            @Valid @RequestBody CreateTopicRequest request) {
        return GlobalResponse.success(topics.save(subjectId, null, request.name(), request.active(), null));
    }

    @Operation(summary = "Cập nhật chủ đề của môn học",
            description = "Đổi tên hoặc trạng thái sử dụng bằng phiên bản đã đọc. Câu hỏi đã gắn chủ đề vẫn giữ liên kết khi chủ đề ngừng sử dụng.")
    @ApiResponse(responseCode = "200", description = "Đã lưu chủ đề và trả về phiên bản hiện tại")
    @ApiResponse(responseCode = "412", description = "Phiên bản đã thay đổi. Tải lại danh mục trước khi cập nhật.")
    @PutMapping("/{topicId}")
    public GlobalResponse<ManagedTopicResponse> update(
            @Parameter(description = "ID môn học chứa chủ đề", example = "12") @PathVariable Long subjectId,
            @Parameter(description = "ID chủ đề cần cập nhật", example = "37") @PathVariable Long topicId,
            @Valid @RequestBody UpdateTopicRequest request) {
        return GlobalResponse.success(topics.save(subjectId, topicId, request.name(), request.active(), request.version()));
    }
}
