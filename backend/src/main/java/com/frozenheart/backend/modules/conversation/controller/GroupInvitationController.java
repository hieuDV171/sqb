package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.conversation.dto.GroupCandidateDto;
import com.frozenheart.backend.modules.conversation.dto.GroupInvitationDto;
import com.frozenheart.backend.modules.conversation.service.GroupInvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/conversations/group")
@RequiredArgsConstructor
@Tag(name = "10. Trò chuyện - Cuộc hội thoại (Conversations)")
public class GroupInvitationController {
    private final GroupInvitationService service;

    @GetMapping("/candidates")
    @Operation(summary = "Tìm người có thể thêm hoặc mời vào nhóm",
            description = "Trả về bạn bè, người theo dõi và người đang được theo dõi. Khi có ID nhóm, loại thành viên hiện tại và người đã có lời mời còn hạn.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy danh sách người phù hợp thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "403", description = "Không có quyền truy cập nhóm"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy nhóm")
    })
    public GlobalResponse<CursorResponse<GroupCandidateDto>> candidates(
            @Parameter(description = "ID nhóm hiện có. Bỏ qua khi tạo nhóm.")
            @RequestParam(name = "conversation_id", required = false) Long conversationId,
            @Parameter(description = "Con trỏ ID người dùng từ trang trước")
            @RequestParam(defaultValue = "0") long after,
            @Parameter(description = "Từ khóa tìm theo tên") @RequestParam(defaultValue = "") String search,
            @Parameter(description = "Số người mỗi trang, tối đa 50") @RequestParam(defaultValue = "20") int limit) {
        return GlobalResponse.success(service.candidates(conversationId, after, search, limit));
    }

    @GetMapping("/invitations")
    @Operation(summary = "Xem lời mời vào nhóm gửi tới tài khoản hiện tại",
            description = "Chỉ trả về lời mời còn hiệu lực. Nội dung nhóm chỉ được mở sau khi chấp nhận.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lấy lời mời thành công"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
    })
    public GlobalResponse<CursorResponse<GroupInvitationDto>> incoming(
            @Parameter(description = "Con trỏ ID lời mời từ trang trước") @RequestParam(defaultValue = "0") long after,
            @Parameter(description = "Số lời mời mỗi trang, tối đa 50") @RequestParam(defaultValue = "20") int limit) {
        return GlobalResponse.success(service.incoming(after, limit));
    }

    @PostMapping("/invitations/{id}/accept")
    @Operation(summary = "Chấp nhận lời mời và nhận ID hội thoại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã tham gia nhóm"),
            @ApiResponse(responseCode = "400", description = "Nhóm đã đạt giới hạn thành viên"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lời mời còn hiệu lực của người dùng hiện tại")
    })
    public GlobalResponse<Long> accept(@Parameter(description = "ID lời mời") @PathVariable Long id) {
        return GlobalResponse.success(service.accept(id));
    }

    @DeleteMapping("/invitations/{id}")
    @Operation(summary = "Từ chối lời mời gửi tới tài khoản hiện tại")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Đã từ chối lời mời"),
            @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
            @ApiResponse(responseCode = "404", description = "Không tìm thấy lời mời của người dùng hiện tại")
    })
    public GlobalResponse<Void> decline(@Parameter(description = "ID lời mời") @PathVariable Long id) {
        service.decline(id);
        return GlobalResponse.success("Đã từ chối lời mời", null);
    }
}
