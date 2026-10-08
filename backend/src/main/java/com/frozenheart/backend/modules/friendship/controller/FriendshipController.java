package com.frozenheart.backend.modules.friendship.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.friendship.dto.*;
import com.frozenheart.backend.modules.friendship.service.FriendshipService;
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
@RequestMapping("/friendships")
@RequiredArgsConstructor
@Tag(name = "07. Quan hệ - Kết bạn (Friendships)", description = "Các API gửi lời mời, chấp nhận, từ chối, hủy kết bạn và quản lý danh sách bạn bè đồng cấp (Sinh viên - Sinh viên hoặc Giảng viên - Giảng viên)")
public class FriendshipController {

    private final FriendshipService friendshipService;

    @Operation(summary = "Gửi lời mời kết bạn", description = "Chỉ người dùng đồng cấp (cùng vai trò Sinh viên hoặc Giảng viên) mới có thể kết bạn. Nếu người kia đã gửi lời mời trước đó, hệ thống sẽ tự động chấp nhận kết bạn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Gửi lời mời kết bạn thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể gửi lời mời cho chính mình (CANNOT_INTERACT_WITH_SELF)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Mối quan hệ bị chặn hoặc không cùng vai trò đào tạo (USER_IS_BLOCKED / ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)"),
            @ApiResponse(responseCode = "409", description = "Đã là bạn bè hoặc lời mời đang chờ xử lý (ALREADY_FRIENDS / FRIEND_REQUEST_PENDING)")
    })
    @PostMapping("/request")
    public ResponseEntity<GlobalResponse<FriendshipResponseDto>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestDto request) {
        FriendshipResponseDto response = friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Chấp nhận lời mời kết bạn", description = "Chấp nhận lời mời kết bạn đang ở trạng thái PENDING do người khác gửi đến.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chấp nhận lời mời kết bạn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Mối quan hệ bị chặn hoặc không có quyền (USER_IS_BLOCKED / ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lời mời kết bạn (RESOURCE_NOT_FOUND)")
    })
    @PostMapping("/accept")
    public ResponseEntity<GlobalResponse<FriendshipResponseDto>> acceptFriendRequest(
            @Valid @RequestBody AcceptFriendRequestDto request) {
        FriendshipResponseDto response = friendshipService.acceptFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Từ chối lời mời kết bạn", description = "Từ chối và hủy bỏ lời mời kết bạn đang chờ.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Từ chối lời mời kết bạn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền từ chối lời mời này (ACTION_NOT_ALLOWED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lời mời kết bạn (RESOURCE_NOT_FOUND)")
    })
    @PostMapping("/decline")
    public ResponseEntity<GlobalResponse<Void>> declineFriendRequest(
            @Valid @RequestBody DeclineFriendRequestDto request) {
        friendshipService.declineFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Hủy kết bạn (Unfriend)", description = "Xóa quan hệ bạn bè giữa người dùng hiện tại và người bạn chỉ định.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hủy kết bạn thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Hai người chưa là bạn bè của nhau (RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/{friendId}")
    public ResponseEntity<GlobalResponse<Void>> unfriend(
            @Parameter(description = "ID của người bạn cần hủy kết bạn", example = "5", required = true)
            @PathVariable Long friendId) {
        friendshipService.unfriend(friendId);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @Operation(summary = "Lấy danh sách bạn bè của tôi", description = "Trả về danh sách bạn bè đã được kết nối kèm phân trang cursor.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách bạn bè thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping
    public ResponseEntity<GlobalResponse<FriendListResponseDto>> getMyFriends(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp trang sau", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng bạn bè mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendListResponseDto response = friendshipService.getMyFriends(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách lời mời kết bạn đã nhận", description = "Danh sách các lời mời kết bạn do người khác gửi đến đang chờ bản thân duyệt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lời mời đã nhận thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/requests/received")
    public ResponseEntity<GlobalResponse<FriendRequestReceivedListResponseDto>> getReceivedFriendRequests(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng lời mời mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendRequestReceivedListResponseDto response = friendshipService.getReceivedFriendRequests(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách lời mời kết bạn đã gửi", description = "Danh sách các lời mời kết bạn do bản thân gửi đi đang chờ đối phương duyệt.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách lời mời đã gửi thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/requests/sent")
    public ResponseEntity<GlobalResponse<FriendRequestSentListResponseDto>> getSentFriendRequests(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng lời mời mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendRequestSentListResponseDto response = friendshipService.getSentFriendRequests(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
