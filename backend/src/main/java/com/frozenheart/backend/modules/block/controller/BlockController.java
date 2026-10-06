package com.frozenheart.backend.modules.block.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.block.dto.*;
import com.frozenheart.backend.modules.block.service.BlockService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blocks")
@RequiredArgsConstructor
@Tag(name = "08. Quan hệ - Chặn (Blocks)", description = "Các API chặn, bỏ chặn người dùng và xem danh sách những tài khoản đang bị chặn")
public class BlockController {

    private final BlockService blockService;

    @Operation(summary = "Chặn một người dùng", description = "Chặn người dùng khác. Hành động này sẽ tự động hủy quan hệ bạn bè và hủy theo dõi 2 chiều giữa 2 người.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chặn người dùng thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể tự chặn chính mình (CANNOT_INTERACT_WITH_SELF)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Bạn đã chặn người dùng này rồi (ALREADY_BLOCKED)")
    })
    @PostMapping("/{targetUserId}")
    public ResponseEntity<GlobalResponse<BlockResponseDto>> blockUser(
            @Parameter(description = "ID người dùng cần chặn", example = "4", required = true)
            @PathVariable Long targetUserId) {
        BlockResponseDto response = blockService.blockUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Bỏ chặn người dùng", description = "Gỡ trạng thái chặn đối với người dùng đã bị chặn trước đó.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Bỏ chặn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Chưa chặn người dùng này (RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<GlobalResponse<UnblockResponseDto>> unblockUser(
            @Parameter(description = "ID người dùng cần bỏ chặn", example = "4", required = true)
            @PathVariable Long targetUserId) {
        UnblockResponseDto response = blockService.unblockUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách người dùng đang bị chặn", description = "Trả về danh sách các tài khoản mà người dùng hiện tại đang chặn kèm phân trang cursor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách chặn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<BlockedListResponseDto>> getBlockedUsers(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        BlockedListResponseDto response = blockService.getBlockedUsers(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
