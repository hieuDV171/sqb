package com.frozenheart.backend.modules.gamification.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.gamification.dto.RebuildLeaderboardRequest;
import com.frozenheart.backend.modules.gamification.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/leaderboards")
@RequiredArgsConstructor
public class AdminLeaderboardController {

    private final LeaderboardService leaderboardService;

    @PostMapping("/rebuild")
    public ResponseEntity<GlobalResponse<Void>> rebuildLeaderboard(
            @RequestBody RebuildLeaderboardRequest request
            ) {
        leaderboardService.rebuildLeaderboard(request.type(), request.subjectId());

        return ResponseEntity.ok(GlobalResponse.success());
    }

}
