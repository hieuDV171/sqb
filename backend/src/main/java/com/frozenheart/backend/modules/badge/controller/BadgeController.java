package com.frozenheart.backend.modules.badge.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.badge.dto.BadgeResponseDto;
import com.frozenheart.backend.modules.badge.dto.UserBadgeResponseDto;
import com.frozenheart.backend.modules.badge.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/badges")
@RequiredArgsConstructor
public class BadgeController {

    private final BadgeService badgeService;

    @GetMapping
    public ResponseEntity<GlobalResponse<List<BadgeResponseDto>>> getAllBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getAllBadges()));
    }

    @GetMapping("/me")
    public ResponseEntity<GlobalResponse<List<UserBadgeResponseDto>>> getMyBadges() {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getMyBadges()));
    }

    @GetMapping("/users/{userId}")
    public ResponseEntity<GlobalResponse<List<UserBadgeResponseDto>>> getUserBadges(@PathVariable Long userId) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getUserBadges(userId)));
    }

    @GetMapping("/{badgeId}")
    public ResponseEntity<GlobalResponse<BadgeResponseDto>> getBadgeById(@PathVariable Long badgeId) {
        return ResponseEntity.ok(GlobalResponse.success(badgeService.getBadgeById(badgeId)));
    }
}
