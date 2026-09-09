package com.frozenheart.backend.modules.gamification.service;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.modules.gamification.dto.*;

import java.util.List;

public interface GamificationService {

    GamePredictionResponse predictGame1(Game1PredictionRequest request);

    List<MyCourseClassPredictionDto> getMyCourseClassesForGame1Prediction();

    GamePredictionResponse predictGame2(Game2PredictionRequest request);

    GamePredictionResponse predictGame4(Game4PredictionRequest request);

    Game6ActiveSessionResponse getActiveGame6Session();

    GamePredictionResponse submitGame6(Game6SubmitRequest request);

    Game6ActiveSessionResponse processAndGenerateGame6WeeklySession();

    MyPredictionsResponse getMyPredictions(Long after, Integer limit);

    void resolveGame2And3ForSession(Session session, List<Question> qList);

    void resolveGame1Daily();

    void reviewErrorGame5(Long ratingUserId, Long questionId, ReviewErrorRequest request);

    void finalizeSemester();

    void publishMonthlyLeaderboardHonorPost();

    LeaderboardResponse getLeaderboard(LeaderboardPeriod period, Long subjectId, Long after, Long before, Integer limit);

    CheckInResponse checkInDaily();

}
