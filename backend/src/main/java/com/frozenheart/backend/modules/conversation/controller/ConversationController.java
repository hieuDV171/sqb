package com.frozenheart.backend.modules.conversation.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.conversation.dto.*;
import com.frozenheart.backend.modules.conversation.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping
@RequiredArgsConstructor
public class ConversationController {

    private final ConversationService conversationService;

    @GetMapping("/conversations")
    public ResponseEntity<GlobalResponse<ConversationListResponseDto>> getConversations(
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        ConversationListResponseDto response = conversationService.getConversations(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/conversations/direct")
    public ResponseEntity<GlobalResponse<ConversationDetailDto>> createDirectConversation(
            @Valid @RequestBody CreateDirectConversationDto request) {
        ConversationDetailDto response = conversationService.createDirectConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
    }

    @PostMapping("/conversations/group")
    public ResponseEntity<GlobalResponse<ConversationDetailDto>> createGroupConversation(
            @Valid @RequestBody CreateGroupConversationDto request) {
        ConversationDetailDto response = conversationService.createGroupConversation(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(response));
    }

    @PatchMapping("/conversations/{id}/hide")
    public ResponseEntity<GlobalResponse<Void>> hideConversation(@PathVariable("id") Long conversationId) {
        conversationService.hideConversation(conversationId);
        return ResponseEntity.ok(GlobalResponse.success("Đã ẩn cuộc hội thoại vào kho bí mật", null));
    }

    @PatchMapping("/conversations/{id}/unhide")
    public ResponseEntity<GlobalResponse<Void>> unhideConversation(@PathVariable("id") Long conversationId) {
        conversationService.unhideConversation(conversationId);
        return ResponseEntity.ok(GlobalResponse.success("Đã bỏ ẩn cuộc hội thoại", null));
    }

    @PostMapping({"/users/me/hidden-chat-pin"})
    public ResponseEntity<GlobalResponse<Void>> setHiddenChatPin(@Valid @RequestBody SetHiddenChatPinDto request) {
        conversationService.setHiddenChatPin(request);
        return ResponseEntity.ok(GlobalResponse.success("Cài đặt mã PIN kho ẩn trò chuyện thành công", null));
    }

    @PostMapping("/conversations/hidden/unlock")
    public ResponseEntity<GlobalResponse<Void>> unlockHiddenChat(@Valid @RequestBody UnlockHiddenChatDto request) {
        conversationService.unlockHiddenChat(request);
        return ResponseEntity.ok(GlobalResponse.success("Mở khóa kho ẩn trò chuyện thành công", null));
    }

    @GetMapping("/conversations/hidden")
    public ResponseEntity<GlobalResponse<ConversationListResponseDto>> getHiddenConversations(
            @RequestParam(value = "after", required = false) Long after,
            @RequestParam(value = "limit", required = false, defaultValue = "20") Integer limit) {
        ConversationListResponseDto response = conversationService.getHiddenConversations(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    // --- Group Management Endpoints (5.5.3) ---

    @PutMapping("/conversations/{id}")
    public ResponseEntity<GlobalResponse<ConversationDetailDto>> updateGroupInfo(
            @PathVariable("id") Long conversationId,
            @Valid @RequestBody UpdateGroupInfoDto request) {
        ConversationDetailDto response = conversationService.updateGroupInfo(conversationId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/conversations/{id}/members")
    public ResponseEntity<GlobalResponse<List<ConversationMemberDto>>> getGroupMembers(
            @PathVariable("id") Long conversationId) {
        List<ConversationMemberDto> response = conversationService.getGroupMembers(conversationId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/conversations/{id}/members")
    public ResponseEntity<GlobalResponse<AddGroupMembersResponseDto>> addGroupMembers(
            @PathVariable("id") Long conversationId,
            @Valid @RequestBody AddGroupMembersRequestDto request) {
        AddGroupMembersResponseDto response = conversationService.addGroupMembers(conversationId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/conversations/{id}/members/{userId}")
    public ResponseEntity<GlobalResponse<Void>> removeGroupMember(
            @PathVariable("id") Long conversationId,
            @PathVariable("userId") Long targetUserId) {
        conversationService.removeGroupMember(conversationId, targetUserId);
        return ResponseEntity.ok(GlobalResponse.success("Xóa thành viên khỏi nhóm thành công", null));
    }

    @PutMapping("/conversations/{id}/members/{userId}/role")
    public ResponseEntity<GlobalResponse<Void>> updateMemberRole(
            @PathVariable("id") Long conversationId,
            @PathVariable("userId") Long targetUserId,
            @Valid @RequestBody UpdateMemberRoleRequestDto request) {
        conversationService.updateMemberRole(conversationId, targetUserId, request);
        return ResponseEntity.ok(GlobalResponse.success("Cập nhật vai trò thành viên thành công", null));
    }

    @PostMapping("/conversations/{id}/leave")
    public ResponseEntity<GlobalResponse<LeaveGroupResponseDto>> leaveGroup(
            @PathVariable("id") Long conversationId,
            @RequestBody(required = false) LeaveGroupRequestDto request) {
        LeaveGroupResponseDto response = conversationService.leaveGroup(conversationId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
