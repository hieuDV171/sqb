package com.frozenheart.backend.modules.post.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.post.dto.*;
import com.frozenheart.backend.modules.post.service.PostService;
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
@Tag(name = "04. Mạng xã hội - Bài viết (Posts)", description = "Các API đăng bài viết, video bài giảng, cập nhật, xóa và đọc danh sách bài viết theo dòng thời gian")
public class PostController {

    private final PostService postService;

    @Operation(summary = "Đăng bài viết mới", description = "Hỗ trợ đăng bài viết thảo luận thông thường (SOCIAL_POST) hoặc video bài giảng (LEARNING_VIDEO do Giảng viên/Admin đăng đính kèm subject_id).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đăng bài viết thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ hoặc thiếu môn học cho video bài giảng (MISSING_REQUIRED_PARAMETER / INVALID_PARAMETER_VALUE)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền đăng video bài giảng (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc môn học (USER_NOT_FOUND / INVALID_PARAMETER_VALUE)")
    })
    @PostMapping("/posts")
    public ResponseEntity<GlobalResponse<PostResponseDto>> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        PostResponseDto response = postService.createPost(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách bài viết của bản thân", description = "Lấy danh sách bài viết do chính người dùng hiện tại đăng tải, hỗ trợ phân trang theo con trỏ sau (cursor after).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/posts/me")
    public ResponseEntity<GlobalResponse<CursorResponse<PostResponseDto>>> getMyPosts(
            @Parameter(description = "Con trỏ ID bài viết để lấy trang tiếp theo", example = "105")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng bài viết tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        CursorResponse<PostResponseDto> response = postService.getMyPosts(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách bài viết của người dùng khác", description = "Lấy danh sách bài viết của người dùng theo userId có kiểm tra quyền riêng tư (PUBLIC, FRIENDS) và kiểm tra mối quan hệ chặn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bài viết thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Bị chặn hoặc không có quyền xem bài viết (USER_IS_BLOCKED / ACCESS_DENIED)")
    })
    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<GlobalResponse<CursorResponse<PostResponseDto>>> getUserPosts(
            @Parameter(description = "Mã định danh của người dùng cần xem bài viết", example = "1", required = true)
            @PathVariable Long userId,
            @Parameter(description = "Con trỏ ID bài viết để lấy trang tiếp theo", example = "105")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng bài viết tối đa mỗi trang (mặc định 10, tối đa 50)", example = "10")
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        CursorResponse<PostResponseDto> response = postService.getUserPosts(userId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy chi tiết một bài viết", description = "Xem chi tiết một bài viết theo ID. Có kiểm tra quyền riêng tư và trạng thái chặn giữa 2 tài khoản.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy chi tiết bài viết thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Bài viết riêng tư hoặc người dùng bị chặn (ACCESS_DENIED / USER_IS_BLOCKED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết (POST_NOT_FOUND)")
    })
    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<PostResponseDto>> getPost(
            @Parameter(description = "ID của bài viết", example = "101", required = true)
            @PathVariable Long id) {
        PostResponseDto response = postService.getPostById(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Chỉnh sửa bài viết", description = "Cập nhật nội dung bài viết, tệp đính kèm và chế độ hiển thị. Chỉ tác giả mới có quyền chỉnh sửa.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật bài viết thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền sửa bài viết này (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết (POST_NOT_FOUND)")
    })
    @PutMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<UpdatePostResponseDto>> updatePost(
            @Parameter(description = "ID của bài viết cần chỉnh sửa", example = "101", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostResponseDto response = postService.updatePost(id, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Giảng viên thêm/cập nhật ghi chú chuyên môn", description = "Dành riêng cho Giảng viên hoặc Admin để đính kèm nhận xét học thuật, lời khuyên vào bài đăng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cập nhật ghi chú giảng viên thành công"),
            @ApiResponse(responseCode = "400", description = "Nội dung ghi chú không được để trống"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Chỉ Giảng viên hoặc Admin mới có quyền (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết hoặc tài khoản (POST_NOT_FOUND / USER_NOT_FOUND)")
    })
    @PutMapping("/posts/{id}/lecturer-notes")
    public ResponseEntity<GlobalResponse<Void>> updateLecturerNote(
            @Parameter(description = "ID của bài viết cần thêm ghi chú", example = "101", required = true)
            @PathVariable Long id,
            @Valid @RequestBody UpdateLecturerNoteRequest request) {
        postService.updateLecturerNote(id, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Xóa bài viết", description = "Chỉ tác giả bài viết hoặc Admin mới có quyền xóa. Bài viết sẽ được gắn cờ xóa mềm và dọn dẹp media liên quan.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Xóa bài viết thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền xóa bài viết (ACCESS_DENIED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy bài viết (POST_NOT_FOUND)")
    })
    @DeleteMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<DeletePostResponseDto>> deletePost(
            @Parameter(description = "ID bài viết cần xóa", example = "101", required = true)
            @PathVariable Long id) {
        DeletePostResponseDto response = postService.deletePost(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách video bài giảng", description = "Lấy danh sách các video học tập (LEARNING_VIDEO) đã được Giảng viên đăng tải, có thể lọc theo mã môn học.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách video thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/video-posts")
    public ResponseEntity<GlobalResponse<CursorResponse<VideoPostItemDto>>> getVideoPosts(
            @Parameter(description = "Lọc theo ID môn học", example = "1")
            @RequestParam(name = "subject_id", required = false) Long subjectId,
            @Parameter(description = "Con trỏ ID bài viết để lấy tiếp", example = "50")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng video bài giảng mỗi trang (mặc định 2, tối đa 3)", example = "2")
            @RequestParam(required = false, defaultValue = "2") Integer limit) {
        CursorResponse<VideoPostItemDto> response = postService.getVideoPosts(subjectId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
