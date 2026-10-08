package com.frozenheart.backend.modules.follow.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.follow.dto.*;
import com.frozenheart.backend.modules.follow.service.FollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@Tag(name = "08. Quan hệ - Theo dõi (Follows)", description = "Các API theo dõi, bỏ theo dõi người dùng và xem thống kê quan hệ (Followers, Following, Friends)")
public class FollowController {

    private final FollowService followService;

    @Operation(summary = "Theo dõi một người dùng", description = "Bắt đầu theo dõi hoạt động và bài viết của người dùng khác (không phân biệt sinh viên hay giảng viên).")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Theo dõi người dùng thành công"),
            @ApiResponse(responseCode = "400", description = "Không thể tự theo dõi chính mình (CANNOT_INTERACT_WITH_SELF)"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Mối quan hệ bị chặn (USER_IS_BLOCKED)"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)")
    })
    @PostMapping("/follows/{targetUserId}")
    public ResponseEntity<GlobalResponse<FollowResponseDto>> followUser(
            @Parameter(description = "ID người dùng muốn theo dõi", example = "2", required = true)
            @PathVariable Long targetUserId) {
        FollowResponseDto response = followService.followUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Hủy theo dõi một người dùng", description = "Bỏ theo dõi người dùng đã theo dõi trước đó.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Hủy theo dõi thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng hoặc chưa từng theo dõi (USER_NOT_FOUND / RESOURCE_NOT_FOUND)")
    })
    @DeleteMapping("/follows/{targetUserId}")
    public ResponseEntity<GlobalResponse<UnfollowResponseDto>> unfollowUser(
            @Parameter(description = "ID người dùng muốn hủy theo dõi", example = "2", required = true)
            @PathVariable Long targetUserId) {
        UnfollowResponseDto response = followService.unfollowUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách người tôi đang theo dõi (Following)", description = "Trả về danh sách tài khoản mà người dùng hiện tại đang follow.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/follows/following")
    public ResponseEntity<GlobalResponse<FollowingListResponseDto>> getFollowing(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FollowingListResponseDto response = followService.getFollowing(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Lấy danh sách người đang theo dõi tôi (Followers)", description = "Trả về danh sách tài khoản đang theo dõi người dùng hiện tại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    @GetMapping("/follows/followers")
    public ResponseEntity<GlobalResponse<FollowerListResponseDto>> getFollowers(
            @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000")
            @RequestParam(required = false) Long after,
            @Parameter(description = "Số lượng mỗi trang (mặc định 20, tối đa 50)", example = "20")
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FollowerListResponseDto response = followService.getFollowers(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Operation(summary = "Xem thống kê quan hệ của một người dùng", description = "Xem số lượng bạn bè (friendsCount), số người theo dõi (followersCount) và số người đang theo dõi (followingCount) kèm trạng thái quan hệ với tài khoản hiện tại.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy số liệu thống kê thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)")
    })
    @GetMapping("/users/{userId}/relationship-stats")
    public ResponseEntity<GlobalResponse<RelationshipStatsResponseDto>> getRelationshipStats(
            @Parameter(description = "ID người dùng cần kiểm tra số liệu", example = "2", required = true)
            @PathVariable Long userId) {
        RelationshipStatsResponseDto response = followService.getRelationshipStats(userId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
