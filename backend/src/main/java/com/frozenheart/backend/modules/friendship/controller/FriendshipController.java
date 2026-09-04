package com.frozenheart.backend.modules.friendship.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.friendship.dto.*;
import com.frozenheart.backend.modules.friendship.service.FriendshipService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/friendships")
@RequiredArgsConstructor
public class FriendshipController {

    private final FriendshipService friendshipService;

    @PostMapping("/request")
    public ResponseEntity<GlobalResponse<FriendshipResponseDto>> sendFriendRequest(
            @Valid @RequestBody SendFriendRequestDto request) {
        FriendshipResponseDto response = friendshipService.sendFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/accept")
    public ResponseEntity<GlobalResponse<FriendshipResponseDto>> acceptFriendRequest(
            @Valid @RequestBody AcceptFriendRequestDto request) {
        FriendshipResponseDto response = friendshipService.acceptFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/decline")
    public ResponseEntity<GlobalResponse<Void>> declineFriendRequest(
            @Valid @RequestBody DeclineFriendRequestDto request) {
        friendshipService.declineFriendRequest(request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @DeleteMapping("/{friendId}")
    public ResponseEntity<GlobalResponse<Void>> unfriend(@PathVariable Long friendId) {
        friendshipService.unfriend(friendId);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<FriendListResponseDto>> getMyFriends(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendListResponseDto response = friendshipService.getMyFriends(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/requests/received")
    public ResponseEntity<GlobalResponse<FriendRequestReceivedListResponseDto>> getReceivedFriendRequests(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendRequestReceivedListResponseDto response = friendshipService.getReceivedFriendRequests(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/requests/sent")
    public ResponseEntity<GlobalResponse<FriendRequestSentListResponseDto>> getSentFriendRequests(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        FriendRequestSentListResponseDto response = friendshipService.getSentFriendRequests(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
