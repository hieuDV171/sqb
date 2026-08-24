package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.prediction.*;
import com.frozenheart.backend.core.entity.session.LegacyQuestion;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.gamification.dto.*;
import com.frozenheart.backend.modules.gamification.repository.*;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

import com.frozenheart.backend.modules.session.repository.PointHistoryRepository;
import com.frozenheart.backend.core.entity.session.Semester;
import com.frozenheart.backend.modules.session.repository.SemesterRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.core.entity.user.UserProfile;

import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;

@Slf4j
@Service
@RequiredArgsConstructor
public class GamificationServiceImpl implements GamificationService {

    private final PredictionRepository predictionRepository;
    private final LegacyQuestionRepository legacyQuestionRepository;
    private final Game6LlmQuestionRepository game6LlmQuestionRepository;
    private final MinigameLlmSessionRepository minigameLlmSessionRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRatingRepository userRatingRepository;
    private final PointHistoryRepository pointHistoryRepository;
    private final CounterMetricsService counterMetricsService;
    private final AnonymizerUtil anonymizerUtil;
    private final CurrentSemesterHolder currentSemesterHolder;

    // =========================================================================
    // GAME 1: Đoán số lượng người tham gia đề xuất ngày mai (Cutoff: 22:00 hôm nay)
    // =========================================================================
    @Override
    @Transactional
    public GamePredictionResponse predictGame1(Game1PredictionRequest request) {
        LocalTime nowTime = LocalTime.now();
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        if (nowTime.isAfter(LocalTime.of(22, 0))) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED, "Hết giờ! Nước mắt em rơi, trò chơi kết thúc");
        }

        User gambler = userRepository.getReferenceById(userId);

        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDateTime targetDate = tomorrow.atStartOfDay();

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_1_PARTICIPANTS);
        prediction.setTargetType("DATE");
        prediction.setTargetDate(targetDate);
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

        Map<String, Object> predData = Map.of("predictedCount", request.predictedCount());
        prediction.setPredictionData(PredictionData.builder().data(predData).build());

        Prediction saved = predictionRepository.save(prediction);

        return GamePredictionResponse.builder()
                .id(saved.getId())
                .gameType(saved.getGameType())
                .targetType(saved.getTargetType())
                .predictionData(predData)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // =========================================================================
    // GAME 2: Đoán số lượng câu do LLM / Con người của mình được duyệt (1 lần/session)
    // =========================================================================
    @Override
    @Transactional
    public GamePredictionResponse predictGame2(Game2PredictionRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

        boolean alreadyPredicted = predictionRepository.existsByGamblerIdAndGameTypeAndTargetId(
                userId, GameType.GAME_2_APPROVED_QUESTIONS, request.sessionId());
        if (alreadyPredicted) {
            throw new AppException(ResponseCode.PREDICTION_ALREADY_PLACED, "Bạn đã cược cho game 2 của phiên này");
        }

        List<Question> questions = questionRepository.findBySessionId(request.sessionId());
        long llmCount = questions.stream().filter(Question::isLlmGenerated).count();
        long humanCount = questions.size() - llmCount;

        if (llmCount == 0 && request.predictedLlmCount() != null && request.predictedLlmCount() > 0) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Số câu LLM = 0, không thể đặt cược câu LLM");
        }
        if (humanCount == 0 && request.predictedHumanCount() != null && request.predictedHumanCount() > 0) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Số câu con người = 0, không thể đặt cược câu con người");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_2_APPROVED_QUESTIONS);
        prediction.setTargetType("SESSION");
        prediction.setTargetId(request.sessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

        Map<String, Object> predData = Map.of(
                "predictedLlmCount", request.predictedLlmCount() != null ? request.predictedLlmCount() : 0,
                "predictedHumanCount", request.predictedHumanCount() != null ? request.predictedHumanCount() : 0
        );
        prediction.setPredictionData(PredictionData.builder().data(predData).build());

        Prediction saved = predictionRepository.save(prediction);

        return GamePredictionResponse.builder()
                .id(saved.getId())
                .gameType(saved.getGameType())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .predictionData(predData)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // =========================================================================
    // GAME 4: Đoán tổng số câu NHD môn học (1 lần/môn)
    // =========================================================================
    @Override
    @Transactional
    public GamePredictionResponse predictGame4(Game4PredictionRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        subjectRepository.findById(request.subjectId())
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND));

        boolean alreadyPredicted = predictionRepository.existsByGamblerIdAndGameTypeAndTargetId(
                userId, GameType.GAME_4_SUBJECT_BANK_SIZE, request.subjectId());
        if (alreadyPredicted) {
            throw new AppException(ResponseCode.PREDICTION_ALREADY_PLACED, "Bạn đã đặt cược số câu NHD cho môn học này");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_4_SUBJECT_BANK_SIZE);
        prediction.setTargetType("SUBJECT");
        prediction.setTargetId(request.subjectId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

        Map<String, Object> predData = Map.of("predictedBankSize", request.predictedBankSize());
        prediction.setPredictionData(PredictionData.builder().data(predData).build());

        Prediction saved = predictionRepository.save(prediction);

        return GamePredictionResponse.builder()
                .id(saved.getId())
                .gameType(saved.getGameType())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .predictionData(predData)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    // =========================================================================
    // GAME 6: Lấy phiên đang mở & Submit dự đoán
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Game6ActiveSessionResponse getActiveGame6Session() {
        Optional<MinigameLlmSession> sessionOpt = minigameLlmSessionRepository
                .findFirstByStatusOrderByStartTimeDesc(MinigameLlmSessionStatus.ACTIVE);
        if (sessionOpt.isEmpty()) {
            throw new AppException(ResponseCode.NO_MORE_DATA, "Không có phiên trò chơi chơi nào đang được mở");
        }

        MinigameLlmSession session = sessionOpt.get();
        List<Game6LlmQuestion> questions = game6LlmQuestionRepository.findByIdIn(session.getQuestionIds());

        List<Game6ActiveSessionResponse.Game6QuestionDto> dtos = questions.stream()
                .map(q -> Game6ActiveSessionResponse.Game6QuestionDto.builder()
                        .id(q.getId())
                        .content(q.getContent())
                        .options(q.getOptions())
                        .explanation(q.getExplanation())
                        .imageUrls(q.getImageUrls())
                        .build())
                .toList();

        return Game6ActiveSessionResponse.builder()
                .sessionId(session.getId())
                .weekNumber(session.getWeekNumber())
                .year(session.getYear())
                .startTime(session.getStartTime())
                .endTime(session.getEndTime())
                .questions(dtos)
                .build();
    }

    @Override
    @Transactional
    public GamePredictionResponse submitGame6(Game6SubmitRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        minigameLlmSessionRepository.findById(request.minigameSessionId())
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND));

        boolean alreadySubmitted = predictionRepository.existsByGamblerIdAndGameTypeAndTargetId(
                userId, GameType.GAME_6_LLM_IDENTIFICATION, request.minigameSessionId());
        if (alreadySubmitted) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED, "Bạn đã tham gia dự đoán cho phiên Game 6 này rồi");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_6_LLM_IDENTIFICATION);
        prediction.setTargetType("GAME6_SESSION");
        prediction.setTargetId(request.minigameSessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

        Map<String, Object> predData = Map.of("selectedLlmQuestionIds", request.selectedLlmQuestionIds());
        prediction.setPredictionData(PredictionData.builder().data(predData).build());

        Prediction saved = predictionRepository.save(prediction);

        return GamePredictionResponse.builder()
                .id(saved.getId())
                .gameType(saved.getGameType())
                .targetType(saved.getTargetType())
                .targetId(saved.getTargetId())
                .predictionData(predData)
                .status(saved.getStatus())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<GamePredictionResponse> getMyPredictions() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        List<Prediction> predictions = predictionRepository.findByGamblerId(userId);
        return predictions.stream()
                .map(p -> GamePredictionResponse.builder()
                        .id(p.getId())
                        .gameType(p.getGameType())
                        .targetType(p.getTargetType())
                        .targetId(p.getTargetId())
                        .status(p.getStatus())
                        .predictionData(p.getPredictionData() != null ? p.getPredictionData().getData() : null)
                        .isCorrect(p.isCorrect())
                        .createdAt(p.getCreatedAt())
                        .resolvedAt(p.getResolvedAt())
                        .build())
                .toList();
    }

    // =========================================================================
    // AUTO-RESOLVE GAME 2 & GAME 3 Inside checkAndAutoResolveSessions
    // =========================================================================
    @Override
    @Transactional
    public void resolveGame2And3ForSession(Session session, List<Question> qList) {
        if (session == null || qList == null || qList.isEmpty()) return;

        // RESOLVE GAME 2
        long actualApprovedLlm = qList.stream()
                .filter(q -> q.getStatus() == QuestionStatus.APPROVED && q.isLlmGenerated())
                .count();
        long actualApprovedHomoSapiens = qList.stream()
                .filter(q -> q.getStatus() == QuestionStatus.APPROVED && !q.isLlmGenerated())
                .count();

        List<Prediction> game2Preds = predictionRepository.findByGameTypeAndTargetTypeAndTargetIdFetchGambler(
                GameType.GAME_2_APPROVED_QUESTIONS, "SESSION", session.getId());

        for (Prediction pred : game2Preds) {
            pred.setStatus(PredictionStatus.RESOLVED);
            pred.setResolvedAt(LocalDateTime.now());

            boolean exact = false;
            if (pred.getPredictionData() != null && pred.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
                Object llmVal = dataMap.get("predictedLlmCount");
                Object humanVal = dataMap.get("predictedHumanCount");

                long predLlm = llmVal instanceof Number ? ((Number) llmVal).longValue() : 0;
                long predHuman = humanVal instanceof Number ? ((Number) humanVal).longValue() : 0;

                exact = (predLlm == actualApprovedLlm && predHuman == actualApprovedHomoSapiens);
            }

            pred.setCorrect(exact);
            if (exact) {
                Subject subject = session.getSubject();
                counterMetricsService.awardPublicPoints(
                        pred.getGambler().getId(),
                        Point.GAME2_AWARD.getPoints(),
                        "GAME_2_WIN",
                        "SESSION",
                        session.getId(),
                        subject
                );
            }
        }

        // RESOLVE GAME 3 (Author confidence prediction if session >= 10 questions)
        if (qList.size() >= 10 && session.getProposer() != null) {
            List<Question> first10 = qList.subList(0, 10);
            int accurateCount = 0;

            for (Question q : first10) {
                Double declaredConfidence = q.getConfidenceScore();
                Double avgRating = q.getAvgRating();
                if (declaredConfidence != null && avgRating != null) {
                    double diff = Math.abs(declaredConfidence - avgRating);
                    if (diff <= 0.5) {
                        accurateCount++;
                    }
                }
            }

            if (accurateCount >= 5) { // >= 50% accurate
                Subject subject = session.getSubject();
                counterMetricsService.awardPublicPoints(
                        session.getProposer().getId(),
                        Point.GAME3_AWARD.getPoints(),
                        "GAME_3_AUTHOR_CONFIDENCE_WIN",
                        "SESSION",
                        session.getId(),
                        subject
                );
            }
        }
    }

    // =========================================================================
    // DAILY CRONJOB FOR GAME 1 (Runs daily 00:05)
    // =========================================================================
    @Override
    @Transactional
    public void resolveGame1Daily() {
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<Prediction> preds = predictionRepository.findByGameTypeAndTargetDateBetweenFetchGambler(
                GameType.GAME_1_PARTICIPANTS,
                startOfYesterday,
                startOfToday.minusNanos(1)
        );

        long actualProposers = sessionRepository.countDistinctProposersBetween(startOfYesterday, startOfToday);

        for (Prediction p : preds) {
            p.setStatus(PredictionStatus.RESOLVED);
            p.setResolvedAt(LocalDateTime.now());

            boolean isCorrect = false;
            if (p.getPredictionData() != null && p.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
                Object countVal = dataMap.get("predictedCount");
                if (countVal instanceof Number num) {
                    long predicted = num.longValue();
                    long allowedMargin = Math.max(1, Math.round(actualProposers * 0.05)); // Sai số +-5%
                    isCorrect = Math.abs(predicted - actualProposers) <= allowedMargin;
                }
            }

            p.setCorrect(isCorrect);
            if (isCorrect) {
                counterMetricsService.awardPublicPoints(
                        p.getGambler().getId(),
                        Point.GAME1_AWARD.getPoints(),
                        "GAME_1_WIN",
                        "DATE",
                        null,
                        null
                );
            }
        }
    }

    // =========================================================================
    // GAME 5: GV / ADMIN DUYỆT BÁO LỖI
    // =========================================================================
    @Override
    @Transactional
    public void reviewErrorGame5(Long ratingUserId, Long questionId, ReviewErrorRequest request) {
        UserRating userRating = userRatingRepository.findByUserIdAndRatedQuestionIdFetchQuestionAndSession(ratingUserId, questionId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND));

        if (!userRating.isError()) {
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Đánh giá này không phải là báo lỗi");
        }

        Question question = userRating.getRatedQuestion();
        if (question == null) {
            throw new AppException(ResponseCode.QUESTION_NOT_FOUND);
        }

        if (Boolean.TRUE.equals(request.isComfirmed()) && question.getStatus() == QuestionStatus.APPROVED) {
            Subject subject = (question.getSession() != null) ? question.getSession().getSubject() : null;

            counterMetricsService.awardSecretPoints(
                    ratingUserId,
                    Point.GAME5_REPORT_AWARD.getPoints(),
                    "GAME_5_REPORT_ERROR_APPROVED",
                    "QUESTION",
                    questionId,
                    subject
            );
            // TODO [NOTIFICATION]: Gửi thông báo tổng kết cho người báo lỗi vào cuối kỳ (khi secret_points chuyển thành public_points)

            if (question.getSession() != null && question.getSession().getProposer() != null) {
                Long authorId = question.getSession().getProposer().getId();
                counterMetricsService.deductPublicPoints(
                        authorId,
                        Point.GAME5_AUTHOR_PENALTY.getPoints(),
                        "GAME_5_QUESTION_ERROR_PENALTY",
                        "QUESTION",
                        questionId,
                        subject
                );
                // TODO [NOTIFICATION]: Gửi thông báo ngay lập tức cho tác giả câu hỏi bị trừ điểm
            }
        }
    }

    // =========================================================================
    // ADMIN / CRONJOB: TỔNG KẾT CUỐI KỲ (FINALIZE SEMESTER)
    // =========================================================================
    @Override
    @Transactional
    public void finalizeSemester() {
        counterMetricsService.finalizeSemesterPoints();

        // =========================================================================
        // TỔNG KẾT GAME 4
        // =========================================================================
        List<Prediction> game4Preds = predictionRepository.findByGameTypeAndStatusFetchGambler(
                GameType.GAME_4_SUBJECT_BANK_SIZE, PredictionStatus.PENDING);

        Map<Long, List<Prediction>> predsBySubject = new HashMap<>();
        for (Prediction p : game4Preds) {
            predsBySubject.computeIfAbsent(p.getTargetId(), k -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<Long, List<Prediction>> entry : predsBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<Prediction> preds = entry.getValue();

            long actualBankSize = questionRepository.countBySessionSubjectIdAndStatus(subjectId, QuestionStatus.APPROVED);

            preds.sort((p1, p2) -> {
                long diff1 = getBankSizeDiff(p1, actualBankSize);
                long diff2 = getBankSizeDiff(p2, actualBankSize);
                if (diff1 != diff2) {
                    return Long.compare(diff1, diff2);
                }
                return p1.getCreatedAt().compareTo(p2.getCreatedAt());
            });

            int winnersCount = Math.min(3, preds.size());
            Subject subjectRef = subjectRepository.getReferenceById(subjectId);

            for (int i = 0; i < preds.size(); i++) {
                Prediction p = preds.get(i);
                p.setStatus(PredictionStatus.RESOLVED);
                p.setResolvedAt(LocalDateTime.now());

                boolean isWinner = i < winnersCount;
                p.setCorrect(isWinner);

                if (isWinner) {
                    counterMetricsService.awardPublicPoints(
                            p.getGambler().getId(),
                            Point.GAME4_AWARD.getPoints(),
                            "GAME_4_TOP3_WINNER",
                            "SUBJECT",
                            subjectId,
                            subjectRef
                    );
                }
            }
        }

        // Migrate các câu APPROVED sang legacy_questions kèm imageUrls
        List<Question> approvedQuestions = questionRepository.findByStatusFetchMedias(QuestionStatus.APPROVED);

        List<LegacyQuestion> legacyToSave = approvedQuestions.stream()
                .map(q -> {
                    List<String> imageUrls = (q.getOwnedMedias() != null)
                            ? q.getOwnedMedias().stream()
                                    .map(QuestionMedia::getUrl)
                                    .filter(Objects::nonNull)
                                    .toList()
                            : List.of();
                    return LegacyQuestion.builder()
                            .content(q.getContent())
                            .options(q.getOptions())
                            .explanation(q.getExplanation())
                            .imageUrls(imageUrls)
                            .llmGenerated(q.isLlmGenerated())
                            .used(false)
                            .importedAt(LocalDateTime.now())
                            .build();
                })
                .toList();

        legacyQuestionRepository.saveAll(legacyToSave);

        // Reset toàn bộ is_used = false trong legacy_questions cho học kỳ mới
        legacyQuestionRepository.resetAllIsUsedToFalse();

        log.info("[GamificationService] Finalized semester successfully.");
    }

    private long getBankSizeDiff(Prediction p, long actualBankSize) {
        if (p.getPredictionData() != null && p.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
            Object countVal = dataMap.get("predictedBankSize");
            if (countVal instanceof Number num) {
                return Math.abs(num.longValue() - actualBankSize);
            }
        }
        return Long.MAX_VALUE;
    }

    // =========================================================================
    // GET LEADERBOARD
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LeaderboardPeriod period, Long subjectId, Integer after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageLimit = Math.clamp(limit != null ? limit : 20, 1, 30);
        int startIndex = (after != null && after >= 0) ? after.intValue() : 0;

        Semester filterSemester = null;
        LocalDateTime fromDate = null;
        if (period == LeaderboardPeriod.LAST_WEEK) {
            fromDate = LocalDateTime.now().minusWeeks(1);
        } else if (period == LeaderboardPeriod.LAST_MONTH) {
            fromDate = LocalDateTime.now().minusMonths(1);
        } else if (period == LeaderboardPeriod.SEMESTER) {
            filterSemester = currentSemesterHolder.getCurrentSemester();
        }

        Pageable pageable = PageRequest.of(startIndex / pageLimit, pageLimit + 1);
        List<Object[]> rawList = pointHistoryRepository.findLeaderboardRaw(subjectId, fromDate, filterSemester, pageable);

        if (rawList == null || rawList.isEmpty()) {
            UserProfile myProfile = userProfileRepository.findById(currentUserId).orElse(null);
            MyRankDto myRank = MyRankDto.builder()
                    .rank(0)
                    .totalPoints(0.0)
                    .topPercent(100.0)
                    .totalParticipants(0)
                    .fullName(myProfile != null ? myProfile.getFullName() : "")
                    .avatarUrl(myProfile != null ? myProfile.getAvatarUrl() : null)
                    .frameUrl(myProfile != null ? myProfile.getAvatarFrameUrl() : null)
                    .build();

            return LeaderboardResponse.builder()
                    .entries(List.of())
                    .myRank(myRank)
                    .pagination(CursorPaginationDto.builder().after(null).hasNext(false).build())
                    .build();
        }

        boolean hasNext = rawList.size() > pageLimit;
        List<Object[]> pagedRaw = hasNext ? rawList.subList(0, pageLimit) : rawList;

        Set<Long> pagedUserIds = pagedRaw.stream()
                .map(row -> (Long) row[0])
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(pagedUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        MyRankDto myRankDto = null;

        for (int i = 0; i < pagedRaw.size(); i++) {
            Object[] row = pagedRaw.get(i);
            Long uId = (Long) row[0];
            double pts = row[1] instanceof Number ? ((Number) row[1]).doubleValue() : 0.0;
            int rank = startIndex + i + 1;

            UserProfile profile = profileMap.get(uId);
            
            String fullName = "Nguoi dung #" + anonymizerUtil.encodeUserId(uId);
            String avatarUrl = profile != null ? profile.getAvatarUrl() : null;
            String frameUrl = profile != null ? profile.getAvatarFrameUrl() : null;
            String userCode = profile != null ? profile.getStudentLecturerCode() : null;

            boolean isCurrent = uId.equals(currentUserId);

            LeaderboardEntryDto entry = LeaderboardEntryDto.builder()
                    .rank(rank)
                    .userId(uId)
                    .userCode(userCode)
                    .fullName(fullName)
                    .avatarUrl(avatarUrl)
                    .frameUrl(frameUrl)
                    .totalPoints(pts)
                    .isCurrentUser(isCurrent)
                    .build();

            entries.add(entry);

            if (isCurrent) {
                myRankDto = MyRankDto.builder()
                        .rank(rank)
                        .totalPoints(pts)
                        .topPercent(0.0)
                        .totalParticipants(0)
                        .fullName(fullName)
                        .avatarUrl(avatarUrl)
                        .frameUrl(frameUrl)
                        .build();
            }
        }

        if (myRankDto == null) {
            UserProfile myProfile = userProfileRepository.findById(currentUserId).orElse(null);
            myRankDto = MyRankDto.builder()
                    .rank(0)
                    .totalPoints(0.0)
                    .topPercent(100.0)
                    .totalParticipants(0)
                    .fullName(myProfile != null ? myProfile.getFullName() : "")
                    .avatarUrl(myProfile != null ? myProfile.getAvatarUrl() : null)
                    .frameUrl(myProfile != null ? myProfile.getAvatarFrameUrl() : null)
                    .build();
        }

        Long nextAfter = hasNext ? (long) (startIndex + pageLimit) : null;

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(nextAfter)
                .hasNext(hasNext)
                .build();

        return LeaderboardResponse.builder()
                .entries(entries)
                .myRank(myRankDto)
                .pagination(pagination)
                .build();
    }

}
