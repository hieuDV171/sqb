package com.frozenheart.backend.modules.gamification.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.gamification.dto.RebuildLeaderboardRequest;
import com.frozenheart.backend.modules.gamification.service.LeaderboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "20. Gamification & Minigames", description = "Các API trò chơi dự đoán học thuật (Game 1, 2, 4, 5, 6), điểm danh hàng ngày và Bảng xếp hạng vinh danh")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/leaderboards")
@RequiredArgsConstructor
public class AdminLeaderboardController {

    private final LeaderboardService leaderboardService;

    @Operation(summary = "Tái tạo Bảng xếp hạng trong Redis (Admin)", description = "Quản trị viên kích hoạt đồng bộ và tính toán lại toàn bộ điểm số trên Redis ZSet cho bảng xếp hạng.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Tái tạo bảng xếp hạng thành công")
    })
    @PostMapping("/rebuild")
    public ResponseEntity<GlobalResponse<Void>> rebuildLeaderboard(
            @RequestBody RebuildLeaderboardRequest request
            ) {
        leaderboardService.rebuildLeaderboard(request.type(), request.subjectId());

        return ResponseEntity.ok(GlobalResponse.success());
    }

}

