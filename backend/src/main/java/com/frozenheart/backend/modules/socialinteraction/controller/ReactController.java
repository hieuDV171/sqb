package com.frozenheart.backend.modules.socialinteraction.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactRequest;
import com.frozenheart.backend.modules.socialinteraction.dto.ReactResponseDto;
import com.frozenheart.backend.modules.socialinteraction.service.ReactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "05. Tương tác - Cảm xúc (Reactions)", description = "Các API thả và gỡ tương tác cảm xúc đa hình (LIKE, LOVE, WOW) trên Bài viết, Bình luận, Câu hỏi...")
public class ReactController {

    private final ReactService reactService;

    @Operation(summary = "Thả hoặc gỡ cảm xúc (Toggle Reaction)", description = "Hỗ trợ 3 loại cảm xúc tích cực chuẩn EdTech: LIKE, LOVE, WOW. Nếu bấm lại cùng loại reaction thì hệ thống sẽ tự động gỡ bỏ (hủy react).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tương tác cảm xúc thành công"),
            @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy đối tượng mục tiêu (POST_NOT_FOUND / RESOURCE_NOT_FOUND / USER_NOT_FOUND)")
    })
    @PostMapping("/react")
    public ResponseEntity<GlobalResponse<ReactResponseDto>> toggleReact(
            @Valid @RequestBody ReactRequest request) {
        ReactResponseDto response = reactService.toggleReact(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
