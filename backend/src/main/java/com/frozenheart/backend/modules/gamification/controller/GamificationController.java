package com.frozenheart.backend.modules.gamification.controller;

import com.frozenheart.backend.core.annotation.Idempotent;
import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.modules.gamification.dto.*;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class GamificationController {

    private final GamificationService gamificationService;

    @GetMapping("/games/prediction/game-1/classes")
    public ResponseEntity<GlobalResponse<List<MyCourseClassPredictionDto>>> getMyCourseClassesForGame1Prediction() {
        List<MyCourseClassPredictionDto> response = gamificationService.getMyCourseClassesForGame1Prediction();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/games/prediction/participants")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame1(
            @Valid @RequestBody Game1PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame1(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/games/prediction/approved-questions")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame2(
            @Valid @RequestBody Game2PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame2(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/games/prediction/bank-size")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> predictGame4(
            @Valid @RequestBody Game4PredictionRequest request) {
        GamePredictionResponse response = gamificationService.predictGame4(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/games/prediction/active-session")
    public ResponseEntity<GlobalResponse<Game6ActiveSessionResponse>> getActiveGame6Session() {
        Game6ActiveSessionResponse response = gamificationService.getActiveGame6Session();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/games/prediction/submit")
    public ResponseEntity<GlobalResponse<GamePredictionResponse>> submitGame6(
            @Valid @RequestBody Game6SubmitRequest request) {
        GamePredictionResponse response = gamificationService.submitGame6(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/games/my-predictions")
    public ResponseEntity<GlobalResponse<MyPredictionsResponse>> getMyPredictions(
            @RequestParam(required = false) Long after,
            @RequestParam(defaultValue = "10") Integer limit) {
        MyPredictionsResponse response = gamificationService.getMyPredictions(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PostMapping("/questions/{questionId}/ratings/{ratingUserId}/review-error")
    public ResponseEntity<GlobalResponse<String>> reviewErrorGame5(
            @PathVariable Long ratingUserId,
            @PathVariable Long questionId,
            @Valid @RequestBody ReviewErrorRequest request) {
        gamificationService.reviewErrorGame5(ratingUserId, questionId, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @PostMapping("/admin/semesters/finalize-semester")
    public ResponseEntity<GlobalResponse<String>> finalizeSemester() {
        gamificationService.finalizeSemester();
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @GetMapping("/games/leaderboard")
    public ResponseEntity<GlobalResponse<LeaderboardResponse>> getLeaderboard(
            @RequestParam(defaultValue = "SEMESTER") LeaderboardPeriod period,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false) Long before,
            @RequestParam(defaultValue = "20") Integer limit) {
        LeaderboardResponse response = gamificationService.getLeaderboard(period, subjectId, after, before, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @Idempotent(keyPrefix = "daily_checkin", expireSeconds = 60)
    @PostMapping("/games/check-in")
    public ResponseEntity<GlobalResponse<CheckInResponse>> checkInDaily() {
        CheckInResponse response = gamificationService.checkInDaily();
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

}
