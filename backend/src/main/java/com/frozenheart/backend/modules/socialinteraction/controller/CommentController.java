package com.frozenheart.backend.modules.socialinteraction.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.modules.socialinteraction.dto.*;
import com.frozenheart.backend.modules.socialinteraction.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "04. Tương tác - Bình luận (Comments)", description = "Các API tạo bình luận, phản hồi bình luận phân cấp (nested replies), cập nhật, xóa và đọc danh sách bình luận")
public class CommentController {

    private final CommentService commentService;

    @Operation(summary = "Tạo bình luận mới hoặc trả lời bình luận", description = "Đăng bình luận vào bài viết (POST), phiên đề xuất (SESSION), hoặc câu hỏi (QUESTION). Hỗ trợ trả lời lồng nhau qua parent_comment_id.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tạo bình luận thành công"),
            @ApiResponse(responseCode = "400", description = "Bình luận phải có nội dung chữ hoặc hình ảnh đính kèm (MISSING_REQUIRED_PARAMETER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tượng mục tiêu hoặc bình luận cha (POST_NOT_FOUND / RESOURCE_NOT_FOUND)")
    })
    @PostMapping("/comments")
    public ResponseEntity<GlobalResponse<CommentResponseDto>> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponseDto response = commentService.createComment(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách bình luận", description = "Lấy danh sách bình luận theo mục tiêu hoặc danh sách câu trả lời của 1 bình luận cha. Hỗ trợ sắp xếp 'newest' (mới nhất) hoặc 'oldest' (theo trình tự thời gian thảo luận bài tập).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bình luận thành công"),
            @ApiResponse(responseCode = "400", description = "Thiếu tham số target_type hoặc target_id khi lấy bình luận gốc (MISSING_REQUIRED_PARAMETER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tượng mục tiêu (POST_NOT_FOUND / RESOURCE_NOT_FOUND)")
    })
    @GetMapping("/comments")
    public ResponseEntity<GlobalResponse<CursorResponse<CommentResponseDto>>> getComments(
            @Parameter(description = "Loại đối tượng (POST, SESSION, QUESTION)", example = "POST")
            @RequestParam(name = "target_type", required = false) InteractionTargetType targetType,
            @Parameter(description = "ID đối tượng mục tiêu", example = "101")
            @RequestParam(name = "target_id", required = false) Long targetId,
            @Parameter(description = "ID bình luận cha nếu muốn lấy danh sách phản hồi", example = "205")
            @RequestParam(name = "parent_comment_id", required = false) Long parentCommentId,
            @Parameter(description = "Thứ tự sắp xếp: 'newest' hoặc 'oldest'", example = "newest")
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @Parameter(description = "Con trỏ ID bình luận để lấy tiếp trang sau", example = "200")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng bình luận mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        CursorResponse<CommentResponseDto> response = commentService.getComments(
                targetType, targetId, parentCommentId, sort, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Chỉnh sửa bình luận", description = "Chỉnh sửa nội dung hoặc ảnh đính kèm của bình luận. Chỉ người tạo bình luận mới có quyền.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật bình luận thành công"),
            @ApiResponse(responseCode = "400", description = "Bình luận phải có chữ hoặc hình ảnh (MISSING_REQUIRED_PARAMETER)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền sửa bình luận này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bình luận (RESOURCE_NOT_FOUND)")
    })
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<GlobalResponse<CommentResponseDto>> updateComment(
            @Parameter(description = "ID bình luận cần chỉnh sửa", example = "205", required = true)
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResponseDto response = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xóa bình luận", description = "Chỉ người tạo bình luận hoặc Admin mới có quyền xóa. Bình luận sẽ bị đánh dấu xóa mềm.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa bình luận thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa bình luận này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bình luận (RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<GlobalResponse<Void>> deleteComment(
            @Parameter(description = "ID bình luận cần xóa", example = "205", required = true)
            @PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
