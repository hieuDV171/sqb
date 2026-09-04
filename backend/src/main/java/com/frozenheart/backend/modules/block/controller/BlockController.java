package com.frozenheart.backend.modules.block.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.block.dto.*;
import com.frozenheart.backend.modules.block.service.BlockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping("/{targetUserId}")
    public ResponseEntity<GlobalResponse<BlockResponseDto>> blockUser(@PathVariable Long targetUserId) {
        BlockResponseDto response = blockService.blockUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<GlobalResponse<UnblockResponseDto>> unblockUser(@PathVariable Long targetUserId) {
        UnblockResponseDto response = blockService.unblockUser(targetUserId);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping
    public ResponseEntity<GlobalResponse<BlockedListResponseDto>> getBlockedUsers(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        BlockedListResponseDto response = blockService.getBlockedUsers(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
