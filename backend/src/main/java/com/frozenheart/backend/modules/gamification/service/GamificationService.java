package com.frozenheart.backend.modules.gamification.service;

import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.modules.gamification.dto.*;

import java.util.List;

public interface GamificationService {

    GamePredictionResponse predictGame1(Game1PredictionRequest request);

    GamePredictionResponse predictGame2(Game2PredictionRequest request);

    GamePredictionResponse predictGame4(Game4PredictionRequest request);

    Game6ActiveSessionResponse getActiveGame6Session();

    GamePredictionResponse submitGame6(Game6SubmitRequest request);

    List<GamePredictionResponse> getMyPredictions();

    void resolveGame2And3ForSession(Session session, List<Question> qList);

    void resolveGame1Daily();

    void reviewErrorGame5(Long ratingUserId, Long questionId, ReviewErrorRequest request);

    void finalizeSemester();

    LeaderboardResponse getLeaderboard(LeaderboardPeriod period, Long subjectId, Integer after, Integer limit);

}
