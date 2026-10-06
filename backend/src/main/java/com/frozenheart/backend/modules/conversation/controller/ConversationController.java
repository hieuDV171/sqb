package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.conversation.dto.*;
import com.frozenheart.backend.modules.conversation.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "09. Trò chuyện - Cuộc hội thoại (Conversations)", description = "Các API nhắn tin trực tiếp (Direct 1-1), nhóm chat, quản trị nhóm (Tù trưởng, Già làng, Dân làng) và kho ẩn hội thoại bảo mật bằng mã PIN")
public class ConversationController {

        private final ConversationService conversationService;

        @Operation(summary = "Lấy danh sách cuộc hội thoại", description = "Trả về danh sách các cuộc hội thoại (1-1 hoặc nhóm) đang hoạt động của người dùng hiện tại kèm số lượng tin nhắn chưa đọc và tin nhắn mới nhất.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách hội thoại thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
        })
        @GetMapping("/conversations")
        public ResponseEntity<GlobalResponse<ConversationListResponseDto>> getConversations(
                        @Parameter(description = "Con trỏ timestamp tin nhắn cuối để lấy tiếp", example = "1728200000000") @RequestParam(value = "after", required = false) Long after,
                        @Parameter(description = "Số lượng hội thoại mỗi trang (mặc định 20, tối đa 50)", example = "20") @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
                ConversationListResponseDto response = conversationService.getConversations(after, limit);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }

        @Operation(summary = "Tạo cuộc trò chuyện trực tiếp 1-1", description = "Khởi tạo cuộc trò chuyện 1-1 với người dùng khác. Nếu cuộc trò chuyện đã tồn tại trước đó, hệ thống sẽ trả về bản ghi hiện có.")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo cuộc trò chuyện 1-1 thành công"),
                        @ApiResponse(responseCode = "400", description = "Không thể tạo cuộc trò chuyện với chính mình (CANNOT_INTERACT_WITH_SELF)"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Không thể tạo trò chuyện do bị chặn (USER_IS_BLOCKED)"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng đối phương (USER_NOT_FOUND)")
        })
        @PostMapping("/conversations/direct")
        public ResponseEntity<GlobalResponse<ConversationDetailDto>> createDirectConversation(
                        @Valid @RequestBody CreateDirectConversationDto request) {
                ConversationDetailDto response = conversationService.createDirectConversation(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
        }

        @Operation(summary = "Tạo nhóm chat mới", description = "Khởi tạo nhóm trò chuyện mới với số lượng từ 3 đến 200 thành viên (bao gồm người tạo). Người tạo sẽ mặc định mang vai trò Tù trưởng (CHIEF).")
        @ApiResponses({
                        @ApiResponse(responseCode = "201", description = "Tạo nhóm chat thành công"),
                        @ApiResponse(responseCode = "400", description = "Quy mô nhóm không hợp lệ (phải từ 3 đến 200 thành viên - CONSTRAINTS_UNSATISFIED)"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Chứa thành viên đang có quan hệ chặn với người tạo (USER_IS_BLOCKED)"),
                        @ApiResponse(responseCode = "404", description = "Có thành viên không tồn tại trong hệ thống (USER_NOT_FOUND)")
        })
        @PostMapping("/conversations/group")
        public ResponseEntity<GlobalResponse<ConversationDetailDto>> createGroupConversation(
                        @Valid @RequestBody CreateGroupConversationDto request) {
                ConversationDetailDto response = conversationService.createGroupConversation(request);
                return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
        }

        @Operation(summary = "Ẩn cuộc hội thoại vào kho bí mật", description = "Đưa cuộc hội thoại vào danh sách ẩn và tự động tắt chuông thông báo (Mute). Để xem lại cần mã PIN bảo mật.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Đã ẩn cuộc hội thoại vào kho bí mật"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "404", description = "Cuộc hội thoại không tồn tại hoặc bạn không tham gia (CONVERSATION_NOT_FOUND)")
        })
        @PatchMapping("/conversations/{id}/hide")
        public ResponseEntity<GlobalResponse<Void>> hideConversation(
                        @Parameter(description = "ID cuộc hội thoại cần ẩn", example = "10", required = true) @PathVariable("id") Long conversationId) {
                conversationService.hideConversation(conversationId);
                return ResponseEntity.ok(GlobalResponse.success("Đã ẩn cuộc hội thoại vào kho bí mật", null));
        }

        @Operation(summary = "Bỏ ẩn cuộc hội thoại", description = "Đưa cuộc hội thoại từ kho bí mật trở lại danh sách trò chuyện thông thường.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Đã bỏ ẩn cuộc hội thoại"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "404", description = "Cuộc hội thoại không tồn tại hoặc bạn không tham gia (CONVERSATION_NOT_FOUND)")
        })
        @PatchMapping("/conversations/{id}/unhide")
        public ResponseEntity<GlobalResponse<Void>> unhideConversation(
                        @Parameter(description = "ID cuộc hội thoại cần bỏ ẩn", example = "10", required = true) @PathVariable("id") Long conversationId) {
                conversationService.unhideConversation(conversationId);
                return ResponseEntity.ok(GlobalResponse.success("Đã bỏ ẩn cuộc hội thoại", null));
        }

        @Operation(summary = "Cài đặt hoặc đổi mã PIN kho trò chuyện ẩn", description = "Thiết lập mã PIN đúng 6 chữ số dùng để bảo vệ và mở khóa kho hội thoại bí mật. Nếu đổi mã cần nhập mã PIN cũ.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cài đặt mã PIN thành công"),
                        @ApiResponse(responseCode = "400", description = "Mã PIN cũ không chính xác hoặc dữ liệu không hợp lệ (INVALID_PIN)"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy hồ sơ người dùng (USER_NOT_FOUND)")
        })
        @PostMapping({ "/users/me/hidden-chat-pin" })
        public ResponseEntity<GlobalResponse<Void>> setHiddenChatPin(
                        @Valid @RequestBody SetHiddenChatPinDto request) {
                conversationService.setHiddenChatPin(request);
                return ResponseEntity.ok(GlobalResponse.success("Cài đặt mã PIN kho ẩn trò chuyện thành công", null));
        }

        @Operation(summary = "Mở khóa kho trò chuyện ẩn", description = "Xác thực mã PIN để truy cập danh sách các cuộc hội thoại đã được ẩn.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Mở khóa kho ẩn thành công"),
                        @ApiResponse(responseCode = "400", description = "Chưa cài mã PIN hoặc mã PIN không đúng (PIN_NOT_SET / INVALID_PIN)"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy người dùng (USER_NOT_FOUND)")
        })
        @PostMapping("/conversations/hidden/unlock")
        public ResponseEntity<GlobalResponse<Void>> unlockHiddenChat(
                        @Valid @RequestBody UnlockHiddenChatDto request) {
                conversationService.unlockHiddenChat(request);
                return ResponseEntity.ok(GlobalResponse.success("Mở khóa kho ẩn trò chuyện thành công", null));
        }

        @Operation(summary = "Lấy danh sách cuộc hội thoại ẩn", description = "Trả về danh sách các cuộc hội thoại nằm trong kho bí mật sau khi đã mở khóa PIN thành công.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách hội thoại ẩn thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn")
        })
        @GetMapping("/conversations/hidden")
        public ResponseEntity<GlobalResponse<ConversationListResponseDto>> getHiddenConversations(
                        @Parameter(description = "Con trỏ timestamp để lấy tiếp", example = "1728200000000") @RequestParam(value = "after", required = false) Long after,
                        @Parameter(description = "Số lượng hội thoại mỗi trang (mặc định 20, tối đa 50)", example = "20") @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
                ConversationListResponseDto response = conversationService.getHiddenConversations(after, limit);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }

        // --- Group Management Endpoints ---

        @Operation(summary = "Cập nhật thông tin nhóm chat", description = "Đổi tên nhóm hoặc ảnh đại diện nhóm. Chỉ Tù trưởng (CHIEF) hoặc Già làng (VILLAGE_ELDER) mới có quyền.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cập nhật thông tin nhóm thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Không tham gia nhóm hoặc không có quyền quản trị (ACTION_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc hội thoại (CONVERSATION_NOT_FOUND)")
        })
        @PutMapping("/conversations/{id}")
        public ResponseEntity<GlobalResponse<ConversationDetailDto>> updateGroupInfo(
                        @Parameter(description = "ID nhóm chat cần cập nhật", example = "12", required = true) @PathVariable("id") Long conversationId,
                        @Valid @RequestBody UpdateGroupInfoDto request) {
                ConversationDetailDto response = conversationService.updateGroupInfo(conversationId, request);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }

        @Operation(summary = "Lấy danh sách thành viên trong nhóm", description = "Xem danh sách toàn bộ thành viên đang hoạt động trong nhóm kèm vai trò (CHIEF, VILLAGE_ELDER, VILLAGER).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Lấy danh sách thành viên thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Bạn không tham gia cuộc hội thoại này (ACTION_NOT_ALLOWED)")
        })
        @GetMapping("/conversations/{id}/members")
        public ResponseEntity<GlobalResponse<List<ConversationMemberDto>>> getGroupMembers(
                        @Parameter(description = "ID nhóm chat", example = "12", required = true) @PathVariable("id") Long conversationId) {
                List<ConversationMemberDto> response = conversationService.getGroupMembers(conversationId);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }

        @Operation(summary = "Thêm thành viên vào nhóm chat", description = "Thêm một hoặc nhiều người dùng vào nhóm (tối đa 200 người). Trả về danh sách thành viên thêm thành công và danh sách thất bại kèm lý do.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Thêm thành viên hoàn tất"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Không có quyền thêm hoặc không thuộc nhóm (ACTION_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc hội thoại (CONVERSATION_NOT_FOUND)")
        })
        @PostMapping("/conversations/{id}/members")
        public ResponseEntity<GlobalResponse<AddGroupMembersResponseDto>> addGroupMembers(
                        @Parameter(description = "ID nhóm chat", example = "12", required = true) @PathVariable("id") Long conversationId,
                        @Valid @RequestBody AddGroupMembersRequestDto request) {
                AddGroupMembersResponseDto response = conversationService.addGroupMembers(conversationId, request);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }

        @Operation(summary = "Xóa thành viên khỏi nhóm chat", description = "Tù trưởng (CHIEF) có quyền xóa Già làng và Dân làng. Già làng (VILLAGE_ELDER) chỉ có quyền xóa Dân làng (VILLAGER).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Xóa thành viên khỏi nhóm thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Không có quyền xóa hoặc thao tác không được phép (ACTION_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Không tìm thấy cuộc hội thoại hoặc thành viên không còn trong nhóm (CONVERSATION_NOT_FOUND / RESOURCE_NOT_FOUND)")
        })
        @DeleteMapping("/conversations/{id}/members/{userId}")
        public ResponseEntity<GlobalResponse<Void>> removeGroupMember(
                        @Parameter(description = "ID nhóm chat", example = "12", required = true) @PathVariable("id") Long conversationId,
                        @Parameter(description = "ID thành viên cần xóa", example = "7", required = true) @PathVariable("userId") Long targetUserId) {
                conversationService.removeGroupMember(conversationId, targetUserId);
                return ResponseEntity.ok(GlobalResponse.success("Xóa thành viên khỏi nhóm thành công", null));
        }

        @Operation(summary = "Cập nhật vai trò thành viên nhóm", description = "Chỉ Tù trưởng (CHIEF) mới có quyền đổi vai trò thành viên (bổ nhiệm VILLAGE_ELDER hoặc hạ xuống VILLAGER). Nếu bổ nhiệm CHIEF mới, Tù trưởng cũ sẽ tự động lùi về làm Già làng (VILLAGE_ELDER).")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Cập nhật vai trò thành viên thành công"),
                        @ApiResponse(responseCode = "400", description = "Dữ liệu yêu cầu không hợp lệ"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Chỉ Tù trưởng mới có quyền thay đổi vai trò (ACTION_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Thành viên không tồn tại trong nhóm (RESOURCE_NOT_FOUND)")
        })
        @PutMapping("/conversations/{id}/members/{userId}/role")
        public ResponseEntity<GlobalResponse<Void>> updateMemberRole(
                        @Parameter(description = "ID nhóm chat", example = "12", required = true) @PathVariable("id") Long conversationId,
                        @Parameter(description = "ID thành viên cần đổi vai trò", example = "7", required = true) @PathVariable("userId") Long targetUserId,
                        @Valid @RequestBody UpdateMemberRoleRequestDto request) {
                conversationService.updateMemberRole(conversationId, targetUserId, request);
                return ResponseEntity.ok(GlobalResponse.success("Cập nhật vai trò thành viên thành công", null));
        }

        @Operation(summary = "Rời khỏi nhóm chat", description = "Tự rời khỏi nhóm. Nếu Tù trưởng rời nhóm, có thể chỉ định người kế nhiệm hoặc hệ thống sẽ tự động chuyển quyền theo thứ tự ưu tiên: Già làng lâu năm nhất -> Dân làng tham gia sớm nhất.")
        @ApiResponses({
                        @ApiResponse(responseCode = "200", description = "Rời nhóm thành công"),
                        @ApiResponse(responseCode = "401", description = "Chưa xác thực hoặc token hết hạn"),
                        @ApiResponse(responseCode = "403", description = "Bạn không tham gia hoặc đã rời nhóm (ACTION_NOT_ALLOWED)"),
                        @ApiResponse(responseCode = "404", description = "Thành viên chỉ định kế nhiệm không hợp lệ (RESOURCE_NOT_FOUND)")
        })
        @PostMapping("/conversations/{id}/leave")
        public ResponseEntity<GlobalResponse<LeaveGroupResponseDto>> leaveGroup(
                        @Parameter(description = "ID nhóm chat cần rời", example = "12", required = true) @PathVariable("id") Long conversationId,
                        @RequestBody(required = false) LeaveGroupRequestDto request) {
                LeaveGroupResponseDto response = conversationService.leaveGroup(conversationId, request);
                return ResponseEntity.ok(GlobalResponse.success(response));
        }
}
