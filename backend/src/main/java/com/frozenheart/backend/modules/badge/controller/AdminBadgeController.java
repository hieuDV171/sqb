package com.frozenheart.backend.modules.badge.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeResponseDto;
import com.frozenheart.backend.modules.badge.dto.CreateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.ManualGrantBadgeRequestDto;
import com.frozenheart.backend.modules.badge.dto.UpdateBadgeRequestDto;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/badges")
@RequiredArgsConstructor
public class AdminBadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<BadgeResponseDto>>> getAllBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getAdminBadges()));
    }

    @PostMapping
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> createBadge(@Valid @RequestBody CreateBadgeRequestDto request) {
        BadgeResponseDto created = badgeService.createBadge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GlobalResponse.success(created));
    }

    @PutMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> updateBadge(
            @PathVariable Long badgeId,
            @Valid @RequestBody UpdateBadgeRequestDto request
    ) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.updateBadge(badgeId, request)));
    }

    @DeleteMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<Void>> deleteBadge(@PathVariable Long badgeId) {
        badgeService.deleteBadge(badgeId);
        return ResponseEntity.ok(GlobalResponse.success(null));
    }

    @PostMapping("/{badgeId}/grant")
    public ResponseEntity<GlobalResponse<Void>> grantManualBadge(
            @PathVariable Long badgeId,
            @Valid @RequestBody ManualGrantBadgeRequestDto request
    ) {
        badgeService.grantManualBadge(badgeId, request);
        return ResponseEntity.ok(GlobalResponse.success(null));
    }
}
