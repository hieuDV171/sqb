package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.constant.Time;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.badge.BadgeTriggerEvent;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.prediction.*;
import com.frozenheart.backend.core.entity.session.*;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.user.*;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.core.util.RedisKeyUtil;
import com.frozenheart.backend.modules.gamification.dto.*;
import com.frozenheart.backend.modules.gamification.repository.*;
import com.frozenheart.backend.modules.gamification.service.GamificationService;
import com.frozenheart.backend.modules.gamification.service.LeaderboardService;
import com.frozenheart.backend.modules.session.repository.*;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.jspecify.annotations.NonNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.core.entity.user.UserProfile;

import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.ai.client.LocalAiClient;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;

import com.frozenheart.backend.core.entity.user.UserGamification;
import com.frozenheart.backend.modules.gamification.repository.UserGamificationRepository;
import com.frozenheart.backend.modules.badge.service.BadgeService;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.time.DayOfWeek;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.frozenheart.backend.core.dto.event.PushNotificationEvent;
import com.frozenheart.backend.core.entity.notification.Notification;
import com.frozenheart.backend.core.entity.notification.NotificationCategory;
import com.frozenheart.backend.core.entity.notification.NotificationMetadata;
import com.frozenheart.backend.core.entity.notification.NotificationTargetType;
import com.frozenheart.backend.core.entity.notification.NotificationType;
import com.frozenheart.backend.core.entity.user.PushNotificationType;
import com.frozenheart.backend.modules.notification.repository.NotificationRepository;
import org.springframework.context.ApplicationEventPublisher;
import com.frozenheart.backend.core.entity.user.CourseClass;
import com.frozenheart.backend.core.entity.user.UserCourseClass;
import com.frozenheart.backend.modules.exam.repository.CourseClassRepository;
import com.frozenheart.backend.modules.user.repository.UserCourseClassRepository;

import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedMetaData;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.user.CoinTransactionTargetType;
import com.frozenheart.backend.core.entity.user.CoinTransactionType;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.user.service.UserCurrencyService;
import java.time.temporal.ChronoUnit;

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
    private final UserGamificationRepository userGamificationRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;
    private final SubjectRepository subjectRepository;
    private final UserRatingRepository userRatingRepository;
    private final CounterMetricsService counterMetricsService;
    private final AnonymizerUtil anonymizerUtil;
    private final CurrentSemesterHolder currentSemesterHolder;
    private final RedisTemplate<String, String> redisTemplate;
    private final LeaderboardService leaderboardService;
    private final SemesterRepository semesterRepository;
    private final LocalAiClient localAiClient;
    private final JsonMapper jsonMapper;
    private final BadgeService badgeService;
    private final CourseClassRepository courseClassRepository;
    private final UserCourseClassRepository userCourseClassRepository;
    private final NotificationRepository notificationRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PointHistoryRepository pointHistoryRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final PostRepository postRepository;
    private final ActivityFeedService activityFeedService;
    private final UserCurrencyService userCurrencyService;

    private static final String GAME6_SYSTEM_PROMPT = """
            Bạn là trợ lý AI chuyên tinh chỉnh và nâng cao chất lượng câu hỏi thi trắc nghiệm.
            Hãy chuẩn hóa, diễn đạt lại nội dung câu hỏi, các lựa chọn và lời giải sao cho mượt mà, rõ ràng và đúng chuẩn sư phạm.

            ⚠️ QUY TẮC BẮT BUỘC:
            1. KHÔNG được tạo/sửa/bình luận về hình ảnh, biểu đồ, sơ đồ.
            2. CHỈ trả về JSON hợp lệ nằm giữa <JSON> và </JSON>.
            3. KHÔNG có text nào ngoài thẻ JSON.

            Định dạng JSON:
            <JSON>
            {
              "content": "...",
              "options": [
                { "key": "A", "content": "...", "isCorrect": true, "mediaUrl": null, "mediaId": null }
              ],
              "explanation": "..."
            }
            </JSON>
            """;

    // =========================================================================
    // GAME 1: Đoán số lượng người tham gia đề xuất ngày mai THEO LỚP HỌC PHẦN (Cutoff: 22:00 hôm nay)
    // =========================================================================
    @Override
    @Transactional
    public GamePredictionResponse predictGame1(Game1PredictionRequest request) {
        ZoneId schoolZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        LocalTime nowTime = LocalTime.now(schoolZone);

        if (nowTime.isAfter(LocalTime.of(22, 0))) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED,
                    "Hết giờ! Nước mắt em rơi, trò chơi kết thúc");
        }

        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        User gambler = userRepository.getReferenceById(userId);

        Long courseClassId = request.courseClassId();
        if (!courseClassRepository.existsById(courseClassId)) {
            throw new AppException(ResponseCode.COURSE_CLASS_NOT_FOUND, "Không tìm thấy lớp học phần");
        }

        if (!userCourseClassRepository.existsByIdUserIdAndIdCourseClassId(userId, courseClassId)) {
            throw new AppException(ResponseCode.USER_NOT_IN_COURSE_CLASS, "Bạn không tham gia lớp học phần này");
        }

        LocalDate schoolTomorrow = LocalDate.now(schoolZone).plusDays(1);
        Instant targetDate = schoolTomorrow.atStartOfDay(schoolZone).toInstant();
        Instant endOfTomorrow = schoolTomorrow.plusDays(1).atStartOfDay(schoolZone).toInstant().minusNanos(1);

        boolean alreadyPredicted = predictionRepository.existsByGamblerIdAndGameTypeAndTargetTypeAndTargetIdAndTargetDateBetween(
                userId, GameType.GAME_1_PARTICIPANTS, PredictionTargetType.COURSE_CLASS, courseClassId, targetDate, endOfTomorrow);
        if (alreadyPredicted) {
            throw new AppException(ResponseCode.PREDICTION_ALREADY_PLACED, "Bạn đã cược cho lớp học này vào ngày mai rồi (Nghiện ngập ít thôi, học bài đi)");
        }

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_1_PARTICIPANTS);
        prediction.setTargetType(PredictionTargetType.COURSE_CLASS);
        prediction.setTargetId(courseClassId);
        prediction.setTargetDate(targetDate);
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(Instant.now());

        Map<String, Object> predData = Map.of(
                "predictedCount", request.predictedCount(),
                "courseClassId", courseClassId);
        return getGamePredictionResponse(prediction, predData);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MyCourseClassPredictionDto> getMyCourseClassesForGame1Prediction() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        ZoneId schoolZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        LocalDate schoolTomorrow = LocalDate.now(schoolZone).plusDays(1);
        Instant targetDate = schoolTomorrow.atStartOfDay(schoolZone).toInstant();
        Instant endOfTomorrow = schoolTomorrow.plusDays(1).atStartOfDay(schoolZone).toInstant().minusNanos(1);

        List<UserCourseClass> enrollments = userCourseClassRepository.findByUserIdFetchCourseClassAndSubject(userId);
        if (enrollments.isEmpty()) {
            return Collections.emptyList();
        }

        List<Prediction> tomorrowPredictions = predictionRepository.findByGamblerIdAndGameTypeAndTargetTypeAndTargetDateBetween(
                userId, GameType.GAME_1_PARTICIPANTS, PredictionTargetType.COURSE_CLASS, targetDate, endOfTomorrow);

        Map<Long, Prediction> predictionMap = tomorrowPredictions.stream()
                .filter(p -> p.getTargetId() != null)
                .collect(Collectors.toMap(Prediction::getTargetId, p -> p, (p1, _) -> p1));

        Set<Long> lecturerIds = enrollments.stream()
                .map(ucc -> ucc.getCourseClass().getLecturer() != null ? ucc.getCourseClass().getLecturer().getId() : null)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, String> lecturerNameMap = lecturerIds.isEmpty()
                ? Collections.emptyMap()
                : userProfileRepository.findAllById(lecturerIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName, (n1, _) -> n1));

        return enrollments.stream().map(ucc -> {
            CourseClass cc = ucc.getCourseClass();
            Long classId = cc.getId();
            Prediction pred = predictionMap.get(classId);
            boolean alreadyPredicted = pred != null;
            Integer predictedCount = null;
            if (alreadyPredicted && pred.getPredictionData() != null && pred.getPredictionData().getData() instanceof Map<?, ?> map) {
                Object val = map.get("predictedCount");
                if (val instanceof Number num) {
                    predictedCount = num.intValue();
                }
            }

            Long lecturerId = cc.getLecturer() != null ? cc.getLecturer().getId() : null;
            String lecturerName = lecturerId != null ? lecturerNameMap.get(lecturerId) : null;

            return MyCourseClassPredictionDto.builder()
                    .courseClassId(classId)
                    .classCode(cc.getClassCode())
                    .semester(cc.getSemester() != null ? cc.getSemester().getName() : null)
                    .subjectId(cc.getSubject() != null ? cc.getSubject().getId() : null)
                    .subjectName(cc.getSubject() != null ? cc.getSubject().getName() : null)
                    .lecturerName(lecturerName)
                    .alreadyPredicted(alreadyPredicted)
                    .predictedCount(predictedCount)
                    .build();
        }).toList();
    }

    // =========================================================================
    // GAME 2: Đoán số lượng câu do LLM / Con người của mình được duyệt (1
    // lần/session)
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
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Số câu con người = 0, không thể đặt cược câu con người");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_2_APPROVED_QUESTIONS);
        prediction.setTargetType(PredictionTargetType.SESSION);
        prediction.setTargetId(request.sessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(Instant.now());

        Map<String, Object> predData = Map.of(
                "predictedLlmCount", request.predictedLlmCount() != null ? request.predictedLlmCount() : 0,
                "predictedHumanCount", request.predictedHumanCount() != null ? request.predictedHumanCount() : 0);
        return getGamePredictionResponse(prediction, predData);
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
            throw new AppException(ResponseCode.PREDICTION_ALREADY_PLACED,
                    "Bạn đã đặt cược số câu NHD cho môn học này");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_4_SUBJECT_BANK_SIZE);
        prediction.setTargetType(PredictionTargetType.SUBJECT);
        prediction.setTargetId(request.subjectId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(Instant.now());

        Map<String, Object> predData = Map.of("predictedBankSize", request.predictedBankSize());
        return getGamePredictionResponse(prediction, predData);
    }

    // =========================================================================
    // GAME 6: Lấy phiên đang mở & Submit dự đoán
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public Game6ActiveSessionResponse getActiveGame6Session() {
        Optional<MinigameLlmSession> sessionOpt = minigameLlmSessionRepository
                .findByStatusFetchQuestions(MinigameLlmSessionStatus.ACTIVE);
        if (sessionOpt.isEmpty()) {
            throw new AppException(ResponseCode.NO_MORE_DATA, "Không có phiên trò chơi nào đang được mở");
        }

        MinigameLlmSession session = sessionOpt.get();
        Set<Game6LlmQuestion> questions = session.getQuestions() != null ? session.getQuestions() : Set.of();

        List<Game6ActiveSessionResponse.Game6QuestionDto> dtos = questions.stream()
                .sorted(Comparator.comparing(Game6LlmQuestion::getId))
                .map(q -> Game6ActiveSessionResponse.Game6QuestionDto.builder()
                        .questionId(q.getId())
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
    public Game6ActiveSessionResponse processAndGenerateGame6WeeklySession() {
        LocalDate today = LocalDate.now(ZoneId.of(Time.DEFAULT_TIMEZONE));
        if (today.getDayOfWeek() != DayOfWeek.SATURDAY) {
            log.warn("[Game 6 Cron] Today is {} (not Saturday). Skipping Game 6 process.", today.getDayOfWeek());
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE,
                    "Game 6 chỉ được xử lý và mở phiên vào ngày Thứ 7");
        }

        // TỔNG KẾT PHIÊN CŨ (NẾU CÓ ACTIVE SESSION)
        Optional<MinigameLlmSession> activeSessionOpt = minigameLlmSessionRepository
                .findFirstByStatusOrderByStartTimeDesc(MinigameLlmSessionStatus.ACTIVE);

        if (activeSessionOpt.isPresent()) {
            MinigameLlmSession activeSession = activeSessionOpt.get();

            Set<Long> actualLlmQuestionIds = getActualLlmQuestionIds(activeSession);

            List<Prediction> predictions = predictionRepository.findByGameTypeAndTargetTypeAndTargetIdFetchGambler(
                    GameType.GAME_6_LLM_IDENTIFICATION, PredictionTargetType.GAME6_SESSION, activeSession.getId());

            for (Prediction pred : predictions) {
                pred.setStatus(PredictionStatus.RESOLVED);
                pred.setResolvedAt(Instant.now());

                boolean isCorrect = false;
                if (pred.getPredictionData() != null
                        && pred.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
                    Object selectedIdsObj = dataMap.get("selectedLlmQuestionIds");
                    Set<Long> userSelectedIds = new HashSet<>();
                    if (selectedIdsObj instanceof List<?> list) {
                        for (Object item : list) {
                            if (item instanceof Number num) {
                                userSelectedIds.add(num.longValue());
                            }
                        }
                    }
                    isCorrect = actualLlmQuestionIds.equals(userSelectedIds);
                }

                pred.setCorrect(isCorrect);

                if (isCorrect) {
                    counterMetricsService.awardPublicPoints(
                            pred.getGambler().getId(),
                            Point.GAME6_AWARD.getPoints(),
                            PointHistoryReason.GAME_6_WIN,
                            PointHistoryTargetType.GAME6_SESSION,
                            activeSession.getId(),
                            null);
                }
            }

            activeSession.setStatus(MinigameLlmSessionStatus.RESOLVED);
            minigameLlmSessionRepository.save(activeSession);
        }

        // SINH ĐỀ VÀ MỞ PHIÊN GAME 6 MỚI (CHO NGÀY THỨ 7)
        // Sampling 5 câu hiện tại bằng Random ID sampling ở Application level
        List<Long> allCurrentIds = new ArrayList<>(questionRepository.findNonApprovedQuestionIds());
        if (allCurrentIds.size() > 5) {
            Collections.shuffle(allCurrentIds);
            allCurrentIds = allCurrentIds.subList(0, 5);
        }
        List<Question> currentQuestions = !allCurrentIds.isEmpty()
                ? questionRepository.findByIdInFetchMedias(allCurrentIds)
                : List.of();

        // Sampling 2 câu legacy bằng Random ID sampling ở Application level
        List<Long> legacyIds = new ArrayList<>(legacyQuestionRepository.findUnusedLegacyQuestionIds());
        if (legacyIds.size() < 2) {
            legacyIds = new ArrayList<>(legacyQuestionRepository.findAllLegacyQuestionIds());
        }
        if (legacyIds.size() > 2) {
            Collections.shuffle(legacyIds);
            legacyIds = legacyIds.subList(0, 2);
        }
        List<LegacyQuestion> legacyQuestions = !legacyIds.isEmpty()
                ? legacyQuestionRepository.findAllById(legacyIds)
                : List.of();

        for (LegacyQuestion lq : legacyQuestions) {
            lq.setUsed(true);
        }
        if (!legacyQuestions.isEmpty()) {
            legacyQuestionRepository.saveAll(legacyQuestions);
        }

        List<CandidateQuestion> candidates = new ArrayList<>();
        for (Question q : currentQuestions) {
            List<String> mediaUrls = (q.getOwnedMedias() != null)
                    ? q.getOwnedMedias().stream()
                            .filter(m -> MediaTarget.CONTENT.equals(m.getMediaTarget()) && m.getUrl() != null)
                            .map(QuestionMedia::getUrl)
                            .toList()
                    : List.of();
            candidates.add(new CandidateQuestion(q.getId(), BankType.CURRENT, q.getContent(), q.getOptions(),
                    q.getExplanation(), mediaUrls));
        }
        for (LegacyQuestion lq : legacyQuestions) {
            candidates.add(new CandidateQuestion(lq.getId(), BankType.LEGACY, lq.getContent(), lq.getOptions(),
                    lq.getExplanation(), lq.getImageUrls()));
        }

        Random random = new Random();
        int k = random.nextInt(candidates.size() + 1); // 0..7
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        Set<Integer> aiIndices = new HashSet<>(indices.subList(0, k));

        ZoneId schoolZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        Instant startTime = today.atStartOfDay(schoolZone).toInstant();
        Instant endTime = today.atTime(23, 59, 59).atZone(schoolZone).toInstant();

        int weekNumber = today.get(WeekFields.ISO.weekOfWeekBasedYear());
        int year = today.getYear();

        MinigameLlmSession newSession = MinigameLlmSession.builder()
                .weekNumber(weekNumber)
                .year(year)
                .startTime(startTime)
                .endTime(endTime)
                .status(MinigameLlmSessionStatus.ACTIVE)
                .build();

        MinigameLlmSession savedSession = minigameLlmSessionRepository.save(newSession);

        List<Game6LlmQuestion> game6QuestionsToSave = new ArrayList<>();

        for (int i = 0; i < candidates.size(); i++) {
            CandidateQuestion cand = candidates.get(i);
            boolean isAiTarget = aiIndices.contains(i);

            String finalContent = cand.content();
            List<QuestionOption> finalOptions = cand.options();
            String finalExplanation = cand.explanation();
            boolean isLlmGenerated = false;

            if (isAiTarget) {
                AiRefineResponse.SuggestedQuestionDto aiResult = refineQuestionWithAi(cand.content(), cand.options(),
                        cand.explanation());
                if (aiResult != null && aiResult.content() != null) {
                    finalContent = aiResult.content();
                    if (aiResult.options() != null && !aiResult.options().isEmpty()) {
                        finalOptions = aiResult.options();
                    }
                    if (aiResult.explanation() != null) {
                        finalExplanation = aiResult.explanation();
                    }
                    isLlmGenerated = true;
                }
            }

            Game6LlmQuestion g6q = Game6LlmQuestion.builder()
                    .gameSession(savedSession)
                    .originalQuestionId(cand.originalId())
                    .sourceType(cand.sourceType())
                    .content(finalContent)
                    .options(finalOptions)
                    .explanation(finalExplanation)
                    .imageUrls(cand.imageUrls())
                    .llmGenerated(isLlmGenerated)
                    .createdAt(Instant.now())
                    .build();

            game6QuestionsToSave.add(g6q);
        }

        List<Game6LlmQuestion> savedGame6Questions = game6LlmQuestionRepository.saveAll(game6QuestionsToSave);

        Map<String, Boolean> correctAnswers = new HashMap<>();
        for (Game6LlmQuestion savedQ : savedGame6Questions) {
            correctAnswers.put(savedQ.getId().toString(), savedQ.isLlmGenerated());
        }

        savedSession.setCorrectAnswers(correctAnswers);
        savedSession.setQuestions(new HashSet<>(savedGame6Questions));
        minigameLlmSessionRepository.save(savedSession);

        List<Game6ActiveSessionResponse.Game6QuestionDto> dtos = savedGame6Questions.stream()
                .map(q -> Game6ActiveSessionResponse.Game6QuestionDto.builder()
                        .questionId(q.getId())
                        .content(q.getContent())
                        .options(q.getOptions())
                        .explanation(q.getExplanation())
                        .imageUrls(q.getImageUrls())
                        .build())
                .toList();

        return Game6ActiveSessionResponse.builder()
                .sessionId(savedSession.getId())
                .weekNumber(savedSession.getWeekNumber())
                .year(savedSession.getYear())
                .startTime(savedSession.getStartTime())
                .endTime(savedSession.getEndTime())
                .questions(dtos)
                .build();
    }

    private @NonNull Set<Long> getActualLlmQuestionIds(MinigameLlmSession activeSession) {
        Map<String, Boolean> correctAnswersMap = activeSession.getCorrectAnswers();
        Set<Long> actualLlmQuestionIds = new HashSet<>();
        if (correctAnswersMap != null) {
            correctAnswersMap.forEach((qIdStr, isLlm) -> {
                if (Boolean.TRUE.equals(isLlm)) {
                    try {
                        actualLlmQuestionIds.add(Long.parseLong(qIdStr));
                    } catch (NumberFormatException ignored) {
                    }
                }
            });
        }
        return actualLlmQuestionIds;
    }

    private AiRefineResponse.SuggestedQuestionDto refineQuestionWithAi(String content, List<QuestionOption> options,
            String explanation) {
        try {
            String optsJson = jsonMapper.writeValueAsString(options != null ? options : List.of());
            String userPrompt = String.format("""
                    Nội dung câu hỏi: %s
                    Các phương án: %s
                    Lời giải: %s
                    Yêu cầu: Câu hỏi sẽ được dùng vào cuộc thi dự đoán xem câu hỏi nào do AI tạo.
                     Bạn hãy chỉnh sửa theo ý bạn sao cho khó đoán ra nhất có thể
                    """, content != null ? content : "", optsJson, explanation != null ? explanation : "");

            LocalAiClient.LocalAiResponse aiResponse = localAiClient.generateCompletion(GAME6_SYSTEM_PROMPT,
                    userPrompt);
            String rawResponse = aiResponse.content();
            if (rawResponse == null)
                return null;

            Pattern pattern = Pattern.compile("<JSON>(.*?)</JSON>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(rawResponse);
            String jsonText = rawResponse;
            if (matcher.find()) {
                jsonText = matcher.group(1).trim();
            }

            Map<String, Object> map = jsonMapper.readValue(jsonText, new TypeReference<>() {
            });
            String refinedContent = (String) map.get("content");
            String refinedExplanation = (String) map.get("explanation");

            List<QuestionOption> refinedOptions = null;
            if (map.get("options") instanceof List<?> rawOpts) {
                refinedOptions = jsonMapper.convertValue(rawOpts, new TypeReference<>() {
                });
            }

            return new AiRefineResponse.SuggestedQuestionDto(refinedContent, refinedOptions, refinedExplanation, null);
        } catch (Exception e) {
            log.warn("[Game 6 AI Refine] Exception while refining question with AI: {}", e.getMessage());
            return null;
        }
    }

    @Override
    @Transactional
    public GamePredictionResponse submitGame6(Game6SubmitRequest request) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        MinigameLlmSession session = minigameLlmSessionRepository.findById(request.minigameSessionId())
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND));

        if (session.getStatus() != MinigameLlmSessionStatus.ACTIVE) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED,
                    "Phiên trò chơi hiện không trong trạng thái mở");
        }

        Instant now = Instant.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED,
                    "Hết giờ! Nước mắt em rơi, trò chơi kết thúc");
        }

        boolean alreadySubmitted = predictionRepository.existsByGamblerIdAndGameTypeAndTargetId(
                userId, GameType.GAME_6_LLM_IDENTIFICATION, request.minigameSessionId());
        if (alreadySubmitted) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED,
                    "Bạn đã tham gia dự đoán cho phiên Game 6 này rồi");
        }

        User gambler = userRepository.getReferenceById(userId);

        Prediction prediction = new Prediction();
        prediction.setGambler(gambler);
        prediction.setGameType(GameType.GAME_6_LLM_IDENTIFICATION);
        prediction.setTargetType(PredictionTargetType.GAME6_SESSION);
        prediction.setTargetId(request.minigameSessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(Instant.now());

        Map<String, Object> predData = Map.of("selectedLlmQuestionIds", request.selectedLlmQuestionIds());
        return getGamePredictionResponse(prediction, predData);
    }

    private GamePredictionResponse getGamePredictionResponse(Prediction prediction, Map<String, Object> predData) {
        prediction.setPredictionData(PredictionData.builder().data(predData).build());

        Prediction saved = predictionRepository.save(prediction);

        return GamePredictionResponse.builder()
                .predictionId(saved.getId())
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
    public MyPredictionsResponse getMyPredictions(Long after, Integer limit) {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageLimit = Math.clamp(limit != null ? limit : 10, 1, 50);

        List<Prediction> predictions = predictionRepository.findByGamblerIdWithCursor(
                userId, after, PageRequest.of(0, pageLimit + 1));

        boolean hasNext = predictions.size() > pageLimit;
        List<Prediction> pageItems = hasNext ? predictions.subList(0, pageLimit) : predictions;

        Long nextCursor = null;
        if (!pageItems.isEmpty() && hasNext) {
            nextCursor = pageItems.getLast().getId();
        }

        List<GamePredictionResponse> dtoList = pageItems.stream()
                .map(p -> GamePredictionResponse.builder()
                        .predictionId(p.getId())
                        .gameType(p.getGameType())
                        .targetType(p.getTargetType())
                        .targetId(p.getTargetId())
                        .status(p.getStatus())
                        .predictionData(p.getPredictionData() != null ? p.getPredictionData().getData() : null)
                        .actualData(p.getActualData() != null ? p.getActualData().getData() : null)
                        .isCorrect(p.isCorrect())
                        .createdAt(p.getCreatedAt())
                        .resolvedAt(p.getResolvedAt())
                        .build())
                .toList();

        return MyPredictionsResponse.builder()
                .contents(dtoList)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    // =========================================================================
    // AUTO-RESOLVE GAME 2 & GAME 3 Inside checkAndAutoResolveSessions
    // =========================================================================
    @Override
    @Transactional
    public void resolveGame2And3ForSession(Session session, List<Question> qList) {
        if (session == null || qList == null || qList.isEmpty())
            return;

        // RESOLVE GAME 2
        long actualApprovedLlm = qList.stream()
                .filter(q -> q.getStatus() == QuestionStatus.APPROVED && q.isLlmGenerated())
                .count();
        long actualApprovedHomoSapiens = qList.stream()
                .filter(q -> q.getStatus() == QuestionStatus.APPROVED && !q.isLlmGenerated())
                .count();

        List<Prediction> game2Preds = predictionRepository.findByGameTypeAndTargetTypeAndTargetIdFetchGambler(
                GameType.GAME_2_APPROVED_QUESTIONS, PredictionTargetType.SESSION, session.getId());

        for (Prediction pred : game2Preds) {
            pred.setStatus(PredictionStatus.RESOLVED);
            pred.setResolvedAt(Instant.now());

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
                        PointHistoryReason.GAME_2_WIN,
                        PointHistoryTargetType.SESSION,
                        session.getId(),
                        subject);
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
                        PointHistoryReason.GAME_3_AUTHOR_CONFIDENCE_WIN,
                        PointHistoryTargetType.SESSION,
                        session.getId(),
                        subject);
            }
        }
    }

    // =========================================================================
    // DAILY CRONJOB FOR GAME 1 (Runs daily 00:05)
    // =========================================================================
    @Override
    @Transactional
    public void resolveGame1Daily() {
        ZoneId schoolZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        LocalDate schoolYesterday = LocalDate.now(schoolZone).minusDays(1);
        Instant startOfYesterday = schoolYesterday.atStartOfDay(schoolZone).toInstant();
        Instant startOfToday = LocalDate.now(schoolZone).atStartOfDay(schoolZone).toInstant();

        List<Prediction> preds = predictionRepository.findByGameTypeAndTargetDateBetweenFetchGambler(
                GameType.GAME_1_PARTICIPANTS,
                startOfYesterday,
                startOfToday.minusNanos(1));

        if (preds.isEmpty()) {
            return;
        }

        Set<Long> classIds = preds.stream()
                .map(Prediction::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Long> classProposerCountMap = new HashMap<>();
        if (!classIds.isEmpty()) {
            List<Object[]> results = sessionRepository.countDistinctProposersByCourseClassesBetween(
                    classIds, startOfYesterday, startOfToday);
            for (Object[] row : results) {
                if (row[0] instanceof Number classIdNum && row[1] instanceof Number countNum) {
                    classProposerCountMap.put(classIdNum.longValue(), countNum.longValue());
                }
            }
        }

        for (Prediction p : preds) {
            p.setStatus(PredictionStatus.RESOLVED);
            p.setResolvedAt(Instant.now());

            Long classId = p.getTargetId();
            long actualProposers = classId != null ? classProposerCountMap.getOrDefault(classId, 0L) : 0L;

            Map<String, Object> actData = new HashMap<>();
            actData.put("actualCount", actualProposers);
            if (classId != null) {
                actData.put("courseClassId", classId);
            }
            p.setActualData(PredictionData.builder().data(actData).build());

            boolean isCorrect = false;
            if (actualProposers > 0 && p.getPredictionData() != null
                    && p.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
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
                        PointHistoryReason.GAME_1_WIN,
                        PointHistoryTargetType.COURSE_CLASS,
                        classId,
                        null);
            }
        }
    }

    // =========================================================================
    // GAME 5: GV / ADMIN DUYỆT BÁO LỖI
    // =========================================================================
    @Override
    @Transactional
    public void reviewErrorGame5(Long ratingUserId, Long questionId, ReviewErrorRequest request) {
        UserRating userRating = userRatingRepository
                .findByUserIdAndRatedQuestionIdFetchQuestionAndSession(ratingUserId, questionId)
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
                    PointHistoryReason.GAME_5_REPORT_ERROR_APPROVED,
                    PointHistoryTargetType.QUESTION,
                    questionId,
                    subject);

            if (question.getSession() != null && question.getSession().getProposer() != null) {
                Long authorId = question.getSession().getProposer().getId();
                counterMetricsService.deductPublicPoints(
                        authorId,
                        Point.GAME5_AUTHOR_PENALTY.getPoints(),
                        PointHistoryReason.GAME_5_QUESTION_ERROR_PENALTY,
                        PointHistoryTargetType.QUESTION,
                        questionId,
                        subject);

                String subjectName = (subject != null) ? subject.getName() : "Chung";
                Notification authorNotif = Notification.builder()
                        .receiver(question.getSession().getProposer())
                        .actor(userRepository.getReferenceById(ratingUserId))
                        .type(NotificationType.QUESTION_ERROR_PENALTY)
                        .category(NotificationCategory.ACADEMIC)
                        .title("Câu hỏi bị trừ điểm do phát hiện sai sót")
                        .body("Câu hỏi của bạn tại môn [" + subjectName + "] đã bị trừ "
                                + Point.GAME5_AUTHOR_PENALTY.getPoints() + " điểm do có sai sót được xác nhận bởi Giảng viên.")
                        .targetType(NotificationTargetType.QUESTION)
                        .targetId(questionId)
                        .targetUrl("/questions/" + questionId)
                        .creadtedAt(Instant.now())
                        .build();
                notificationRepository.save(authorNotif);

                eventPublisher.publishEvent(PushNotificationEvent.single(
                        authorId,
                        "Câu hỏi bị trừ điểm do phát hiện sai sót",
                        "Câu hỏi của bạn tại môn [" + subjectName + "] đã bị trừ "
                                + Point.GAME5_AUTHOR_PENALTY.getPoints() + " điểm do có sai sót được xác nhận bởi Giảng viên.",
                        null,
                        NotificationTargetType.QUESTION,
                        questionId,
                        "/questions/" + questionId,
                        PushNotificationType.ACADEMIC
                ));
            }
        }
    }

    // =========================================================================
    // ADMIN / CRONJOB: TỔNG KẾT CUỐI KỲ (FINALIZE SEMESTER)
    // =========================================================================
    @Override
    @Transactional
    public void finalizeSemester() {
        Semester current = currentSemesterHolder.getCurrentSemester();
        if (current == null) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, "Không thể chốt sổ! Hiện tại không có học kỳ nào đang được kích hoạt trong hệ thống.");
        }
        if (current.isFinalized()) {
            throw new AppException(ResponseCode.ACTION_NOT_ALLOWED, String.format("Học kỳ '%s' đang hoạt động đã được chốt sổ trước đó.", current.getName()));
        }

        counterMetricsService.finalizeSemesterPoints();

        // Gửi thông báo tổng kết Game 5 (Recap) cho những người báo lỗi chính xác trong kỳ
        sendGame5SemesterRecapNotifications(current.getId());

        // =========================================================================
        // TỔNG KẾT GAME 4
        // =========================================================================
        List<Prediction> game4Preds = predictionRepository.findByGameTypeAndStatusFetchGambler(
                GameType.GAME_4_SUBJECT_BANK_SIZE, PredictionStatus.PENDING);

        Map<Long, List<Prediction>> predsBySubject = new HashMap<>();
        for (Prediction p : game4Preds) {
            predsBySubject.computeIfAbsent(p.getTargetId(), _ -> new ArrayList<>()).add(p);
        }

        for (Map.Entry<Long, List<Prediction>> entry : predsBySubject.entrySet()) {
            Long subjectId = entry.getKey();
            List<Prediction> preds = entry.getValue();

            long actualBankSize = questionRepository.countBySessionSubjectIdAndStatus(subjectId,
                    QuestionStatus.APPROVED);

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
                p.setResolvedAt(Instant.now());

                boolean isWinner = i < winnersCount;
                p.setCorrect(isWinner);

                if (isWinner) {
                    counterMetricsService.awardPublicPoints(
                            p.getGambler().getId(),
                            Point.GAME4_AWARD.getPoints(),
                            PointHistoryReason.GAME_4_TOP3_WINNER,
                            PointHistoryTargetType.SUBJECT,
                            subjectId,
                            subjectRef);
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
                            .importedAt(Instant.now())
                            .build();
                })
                .toList();

        legacyQuestionRepository.saveAll(legacyToSave);

        // Reset toàn bộ is_used = false trong legacy_questions cho học kỳ mới
        legacyQuestionRepository.resetAllIsUsedToFalse();

        // Trao huy hiệu thứ hạng Leaderboard cho các sinh viên đạt Top cuối kỳ (Chống N+1)
        try {
            String redisKey = RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SEMESTER, current.getId(), null);
            Set<ZSetOperations.TypedTuple<String>> topRanks = redisTemplate.opsForZSet()
                    .reverseRangeWithScores(redisKey, 0, 9);
            if (topRanks != null && !topRanks.isEmpty()) {
                Map<Long, Integer> userRankMap = createUserRankMap(topRanks);

                List<User> winners = userRepository.findAllById(userRankMap.keySet());
                if (!winners.isEmpty()) {
                    badgeService.checkAndGrantBadgesBatch(winners, BadgeTriggerEvent.LEADERBOARD_RANK, userRankMap);
                }
            }
        } catch (Exception e) {
            log.error("[GamificationService] Lỗi khi trao huy hiệu Leaderboard Rank cuối kỳ: {}", e.getMessage());
        }

        // Tạo bài Post vinh danh Thủ khoa / Top 3 cuối kỳ kèm thưởng Xu và Notification
        try {
            publishLeaderboardHonorPost(current, true);
        } catch (Exception e) {
            log.error("[GamificationService] Lỗi khi tạo bài post vinh danh cuối kỳ: {}", e.getMessage());
        }

        current.setFinalized(true);
        semesterRepository.save(current);

        leaderboardService.scheduleOldSemesterCleanup(currentSemesterHolder.getCurrentSemester().getId());

        log.info("[GamificationService] Finalized semester successfully.");
    }

    private @NonNull Map<Long, Integer> createUserRankMap(Set<ZSetOperations.TypedTuple<String>> topRanks) {
        Map<Long, Integer> userRankMap = new HashMap<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : topRanks) {
            if (tuple.getValue() != null) {
                try {
                    Long winnerId = Long.parseLong(tuple.getValue());
                    userRankMap.put(winnerId, rank);
                } catch (NumberFormatException ignored) {
                }
            }
            rank++;
        }
        return userRankMap;
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

    private void sendGame5SemesterRecapNotifications(Long semesterId) {
        List<PointHistory> errorHistories = pointHistoryRepository.findApprovedErrorHistoriesInSemester(semesterId);
        if (errorHistories.isEmpty()) {
            return;
        }

        Set<Long> questionIds = errorHistories.stream()
                .map(PointHistory::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Long, List<PointHistory>> userHistoriesMap = errorHistories.stream()
                .filter(p -> p.getUser() != null)
                .collect(Collectors.groupingBy(p -> p.getUser().getId()));

        List<Notification> notificationsToSave = new ArrayList<>();
        Instant now = Instant.now();

        for (Map.Entry<Long, List<PointHistory>> entry : userHistoriesMap.entrySet()) {
            Long userId = entry.getKey();
            List<PointHistory> histories = entry.getValue();

            List<NotificationMetadata.ConfirmedQuestionDto> confirmedList = new ArrayList<>();
            double totalBonus = 0;

            for (PointHistory h : histories) {
                totalBonus += h.getPoints();
                Question q = questionMap.get(h.getTargetId());
                String subjectName = (q != null && q.getSession() != null && q.getSession().getSubject() != null)
                        ? q.getSession().getSubject().getName()
                        : "Môn học";
                String snippet = (q != null && q.getContent() != null)
                        ? (q.getContent().length() > 80 ? q.getContent().substring(0, 80) + "..." : q.getContent())
                        : "Câu hỏi #" + h.getTargetId();

                confirmedList.add(NotificationMetadata.ConfirmedQuestionDto.builder()
                        .questionId(h.getTargetId())
                        .subjectName(subjectName)
                        .questionSnippet(snippet)
                        .points(h.getPoints())
                        .build());
            }

            Notification notif = Notification.builder()
                    .receiver(userRepository.getReferenceById(userId))
                    .actor(null)
                    .type(NotificationType.GAME5_SEMESTER_RECAP)
                    .category(NotificationCategory.GAMIFICATION)
                    .title("Tổng kết điểm thưởng phát hiện lỗi câu hỏi cuối kỳ")
                    .body("Chúc mừng bạn! Bạn đã phát hiện chính xác " + confirmedList.size()
                            + " câu hỏi có sai sót trong kỳ (+ " + totalBonus + " điểm). Nhấn để xem chi tiết các câu hỏi.")
                    .targetType(NotificationTargetType.USER)
                    .targetId(userId)
                    .metadata(NotificationMetadata.builder().confirmedQuestions(confirmedList).build())
                    .creadtedAt(now)
                    .build();
            notificationsToSave.add(notif);

            eventPublisher.publishEvent(PushNotificationEvent.single(
                    userId,
                    "Tổng kết điểm thưởng phát hiện lỗi câu hỏi cuối kỳ",
                    "Chúc mừng bạn! Bạn đã phát hiện chính xác " + confirmedList.size()
                            + " câu hỏi có sai sót trong kỳ (+ " + totalBonus + " điểm).",
                    null,
                    NotificationTargetType.USER,
                    userId,
                    null,
                    PushNotificationType.GAMIFICATION
            ));
        }

        if (!notificationsToSave.isEmpty()) {
            notificationRepository.saveAll(notificationsToSave);
        }
    }

    // =========================================================================
    // GET LEADERBOARD (Rank-based Cursor 2 chiều: after & before)
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LeaderboardPeriod period, Long subjectId, Long after, Long before, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageLimit = Math.clamp(limit != null ? limit : 20, 1, 50);

        String redisKey = RedisKeyUtil.buildLeaderboardKey(period, currentSemesterHolder.getCurrentSemester().getId(),
                subjectId);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        Long totalParticipants = zSetOps.zCard(redisKey);
        if (totalParticipants == null)
            totalParticipants = 0L;

        MyRankDto myRankDto = buildMyRankDto(currentUserId, redisKey, totalParticipants, zSetOps);

        if (totalParticipants == 0) {
            return buildEmptyResponse(myRankDto);
        }

        long startIndex = 0;
        if (before != null && before > 1) {
            // Lùi về trang trước: before là Rank bắt đầu của trang hiện tại (1-based)
            startIndex = Math.max(0, before - pageLimit - 1);
        } else if (after != null && after >= 1) {
            // Tiến tới trang sau: after là Rank kết thúc của trang hiện tại (1-based)
            startIndex = after;
        }

        if (startIndex >= totalParticipants) {
            return buildEmptyResponse(myRankDto);
        }

        long endIndex = startIndex + pageLimit - 1;
        Set<ZSetOperations.TypedTuple<String>> rawRedisList = zSetOps.reverseRangeWithScores(redisKey, startIndex,
                endIndex);

        if (rawRedisList == null || rawRedisList.isEmpty()) {
            return buildEmptyResponse(myRankDto);
        }

        List<ZSetOperations.TypedTuple<String>> pagedRaw = new ArrayList<>(rawRedisList);

        Set<Long> pagedUserIds = pagedRaw.stream()
                .map(tuple -> tuple.getValue() != null ? Long.parseLong(tuple.getValue()) : 0L)
                .filter(id -> id > 0)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profileMap = pagedUserIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(pagedUserIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        long currentRank = startIndex + 1;

        for (ZSetOperations.TypedTuple<String> tuple : pagedRaw) {
            Long uId = 0L;
            if (tuple.getValue() != null) {
                uId = Long.valueOf(tuple.getValue());
            }
            double pts = 0;
            if (tuple.getScore() != null) {
                pts = Math.floor(tuple.getScore()) / 100.0;
            }

            UserProfile profile = profileMap.get(uId);
            String fullName = profile != null ? profile.getFullName()
                    : anonymizerUtil.encodeUserId(uId);

            entries.add(LeaderboardEntryDto.builder()
                    .rank((int) currentRank++)
                    .userId(uId)
                    .userCode(profile != null ? profile.getStudentLecturerCode() : null)
                    .fullName(fullName)
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .totalPoints(pts)
                    .isCurrentUser(uId.equals(currentUserId))
                    .build());
        }

        boolean hasPrev = startIndex > 0;
        boolean hasNext = (startIndex + pageLimit) < totalParticipants;

        Long nextAfter = hasNext ? (startIndex + entries.size()) : null;
        Long prevBefore = hasPrev ? (startIndex + 1) : null;

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .before(prevBefore)
                .after(nextAfter)
                .hasPrev(hasPrev)
                .hasNext(hasNext)
                .build();

        return LeaderboardResponse.builder()
                .entries(entries)
                .myRank(myRankDto)
                .pagination(pagination)
                .build();
    }

    // Hàm phụ trợ giúp code gọn gàng hơn
    private LeaderboardResponse buildEmptyResponse(MyRankDto myRankDto) {
        return LeaderboardResponse.builder()
                .entries(List.of())
                .myRank(myRankDto)
                .pagination(CursorPaginationDto.builder().before(null).after(null).hasPrev(false).hasNext(false).build())
                .build();
    }

    private MyRankDto buildMyRankDto(Long userId, String redisKey, Long totalParticipants,
            ZSetOperations<String, String> zSetOps) {
        UserProfile myProfile = userProfileRepository.findById(userId).orElse(null);
        String member = String.valueOf(userId);

        Long myZeroBasedRank = zSetOps.reverseRank(redisKey, member);

        if (myZeroBasedRank == null) {
            // User chưa có điểm nào
            return MyRankDto.builder()
                    .rank(0)
                    .totalPoints(0.0)
                    .topPercent(100.0)
                    .totalParticipants(totalParticipants.intValue())
                    .fullName(myProfile != null ? myProfile.getFullName() : "")
                    .avatarUrl(myProfile != null ? myProfile.getAvatarUrl() : null)
                    .frameUrl(myProfile != null ? myProfile.getAvatarFrameUrl() : null)
                    .build();
        }

        int myActualRank = myZeroBasedRank.intValue() + 1;
        Double zScore = zSetOps.score(redisKey, member);
        double actualPoints = Math.floor(zScore) / 100.0;

        // Tính toán lọt Top bao nhiêu phần trăm
        double topPercent = totalParticipants > 0
                ? ((double) myActualRank / totalParticipants) * 100.0
                : 0.0;
        // Làm tròn 2 chữ số thập phân cho topPercent
        topPercent = Math.round(topPercent * 100.0) / 100.0;

        return MyRankDto.builder()
                .rank(myActualRank)
                .totalPoints(actualPoints)
                .topPercent(topPercent)
                .totalParticipants(totalParticipants.intValue())
                .fullName(myProfile != null ? myProfile.getFullName() : "")
                .avatarUrl(myProfile != null ? myProfile.getAvatarUrl() : null)
                .frameUrl(myProfile != null ? myProfile.getAvatarFrameUrl() : null)
                .build();
    }

    @Override
    @Transactional
    public CheckInResponse checkInDaily() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile profile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        String tz = (profile.getTimezone() != null && !profile.getTimezone().isBlank())
                ? profile.getTimezone() : Time.DEFAULT_TIMEZONE;
        ZoneId userZone;
        try {
            userZone = ZoneId.of(tz);
        } catch (Exception e) {
            userZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        }
        LocalDate today = LocalDate.now(userZone);
        UserGamification gamification = userGamificationRepository.findById(userId)
                .orElseGet(() -> UserGamification.builder()
                        .user(profile.getUser())
                        .build());

        LocalDate lastCheckIn = gamification.getLastCheckInDate();
        if (lastCheckIn != null && !lastCheckIn.isBefore(today)) {
            throw new AppException(ResponseCode.ACTION_ALREADY_PERFORMED, "Bạn đã điểm danh hôm nay rồi!");
        }

        int currentStreak = gamification.getCurrentStreak();
        if (lastCheckIn != null && lastCheckIn.equals(today.minusDays(1))) {
            currentStreak += 1;
        } else {
            currentStreak = 1;
        }

        // Thưởng điểm danh: Mốc chu kỳ 7 ngày nhận 5 xu, các ngày bình thường nhận 1 xu
        double coinEarned = (currentStreak % 7 == 0) ? 5.0 : 1.0;
        gamification.setCurrentStreak(currentStreak);
        gamification.setLastCheckInDate(today);
        gamification.setCoinBalance(gamification.getCoinBalance() + coinEarned);
        gamification.setUpdatedAt(Instant.now());

        userGamificationRepository.save(gamification);

        // Ghi nhận sổ cái bất biến (CoinTransaction - Ledger)
        CoinTransaction coinTx = CoinTransaction.builder()
                .user(profile.getUser())
                .amount(coinEarned)
                .balanceAfter(gamification.getCoinBalance())
                .type(CoinTransactionType.DAILY_CHECK_IN)
                .description("Điểm danh hàng ngày: +" + (long) coinEarned + " xu (Chuỗi " + currentStreak + " ngày)")
                .targetType(CoinTransactionTargetType.DAILY_CHECK_IN)
                .targetId(null)
                .createdAt(Instant.now())
                .build();
        coinTransactionRepository.save(coinTx);

        // Kích hoạt tự động kiểm tra và trao huy hiệu chuỗi chuyên cần (STUDY_STREAK)
        badgeService.checkAndGrantBadges(profile.getUser(), BadgeTriggerEvent.STUDY_STREAK, currentStreak);

        return CheckInResponse.builder()
                .coinEarned(coinEarned)
                .currentCoinBalance(gamification.getCoinBalance())
                .currentStreak(currentStreak)
                .checkInDate(today)
                .message("Điểm danh thành công! Bạn nhận được +" + (long) coinEarned + " xu SQB. Chuỗi hiện tại: "
                        + currentStreak + " ngày.")
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CheckInStatusResponse getCheckInStatus() {
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        UserProfile profile = userProfileRepository.findByUserIdWithUser(userId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        String tz = (profile.getTimezone() != null && !profile.getTimezone().isBlank())
                ? profile.getTimezone() : Time.DEFAULT_TIMEZONE;
        ZoneId userZone;
        try {
            userZone = ZoneId.of(tz);
        } catch (Exception e) {
            userZone = ZoneId.of(Time.DEFAULT_TIMEZONE);
        }
        LocalDate today = LocalDate.now(userZone);

        UserGamification gamification = userGamificationRepository.findById(userId)
                .orElse(null);

        if (gamification == null) {
            return CheckInStatusResponse.builder()
                    .currentStreak(0)
                    .hasCheckedInToday(false)
                    .lastCheckInDate(null)
                    .today(today)
                    .build();
        }

        LocalDate lastCheckIn = gamification.getLastCheckInDate();
        boolean hasCheckedInToday = lastCheckIn != null && lastCheckIn.equals(today);

        // Chuỗi streak hợp lệ: nếu hôm nay chưa điểm danh và ngày điểm danh cuối trước ngày hôm qua => streak đã bị đứt
        int validStreak = gamification.getCurrentStreak();
        if (lastCheckIn != null && !hasCheckedInToday && !lastCheckIn.equals(today.minusDays(1))) {
            validStreak = 0;
        }

        return CheckInStatusResponse.builder()
                .currentStreak(validStreak)
                .hasCheckedInToday(hasCheckedInToday)
                .lastCheckInDate(lastCheckIn)
                .today(today)
                .build();
    }

    @Override
    @Transactional
    public void publishMonthlyLeaderboardHonorPost() {
        Semester current = currentSemesterHolder.getCurrentSemester();
        if (current == null) {
            log.warn("[HonorPost] Không tìm thấy học kỳ hiện tại, bỏ qua tạo post vinh danh đầu tháng.");
            return;
        }

        // Kiểm tra điều kiện ngày khai giảng (ngưỡng x = 14 ngày)
        if (current.getStartDate() != null) {
            LocalDate today = LocalDate.ofInstant(Instant.now(), ZoneId.of(Time.DEFAULT_TIMEZONE));
            long daysSinceStart = ChronoUnit.DAYS.between(current.getStartDate(), today);
            if (daysSinceStart < 14) {
                log.info("[HonorPost] Học kỳ '{}' mới bắt đầu được {} ngày (< 14 ngày). Bỏ qua vinh danh đầu tháng.",
                        current.getName(), daysSinceStart);
                return;
            }
        }

        publishLeaderboardHonorPost(current, false);
    }

    private void publishLeaderboardHonorPost(Semester semester, boolean isEndOfSemester) {
        if (semester == null) {
            return;
        }

        String redisKey = RedisKeyUtil.buildLeaderboardKey(LeaderboardPeriod.SEMESTER, semester.getId(), null);
        Set<ZSetOperations.TypedTuple<String>> topRanks = redisTemplate.opsForZSet()
                .reverseRangeWithScores(redisKey, 0, 2);

        if (topRanks == null || topRanks.isEmpty()) {
            log.info("[HonorPost] Bảng xếp hạng học kỳ '{}' rỗng, bỏ qua tạo bài viết vinh danh.", semester.getName());
            return;
        }

        List<LeaderboardWinnerEntry> winners = new ArrayList<>();
        int rank = 1;

        for (ZSetOperations.TypedTuple<String> tuple : topRanks) {
            if (tuple.getValue() != null && tuple.getScore() != null) {
                double points = Math.floor(tuple.getScore()) / 100.0;
                if (points > 0) {
                    try {
                        Long uId = Long.parseLong(tuple.getValue());
                        double coins = calculateHonorCoins(rank, isEndOfSemester);
                        winners.add(new LeaderboardWinnerEntry(uId, rank, points, coins));
                    } catch (NumberFormatException ignored) {}
                }
            }
            rank++;
        }

        if (winners.isEmpty()) {
            log.info("[HonorPost] Không có sinh viên nào có điểm > 0 trong học kỳ '{}', bỏ qua vinh danh.", semester.getName());
            return;
        }

        // Batch fetch profiles (CHỐNG N+1)
        List<Long> winnerIds = winners.stream().map(LeaderboardWinnerEntry::userId).toList();
        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(winnerIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        Instant now = Instant.now();
        LocalDate today = LocalDate.ofInstant(now, ZoneId.of(Time.DEFAULT_TIMEZONE));

        // 1. Thưởng Xu cho các sinh viên qua UserCurrencyService
        for (LeaderboardWinnerEntry w : winners) {
            String coinReason = isEndOfSemester
                    ? "Thưởng vinh danh " + (w.rank() == 1 ? "Thủ khoa" : "Top " + w.rank()) + " chung cuộc học kỳ " + semester.getName()
                    : "Thưởng vinh danh Top " + w.rank() + " Bảng xếp hạng học kỳ " + semester.getName() + " tháng " + today.getMonthValue() + "/" + today.getYear();

            try {
                userCurrencyService.add(
                        w.userId(),
                        w.coins(),
                        CoinTransactionType.LEADERBOARD_REWARD,
                        coinReason,
                        CoinTransactionTargetType.SEMESTER,
                        semester.getId()
                );
            } catch (Exception e) {
                log.error("[HonorPost] Lỗi cộng xu cho user {}: {}", w.userId(), e.getMessage());
            }
        }

        // 2. Tạo Post hệ thống
        String postContent = buildHonorPostContent(semester, winners, profileMap, isEndOfSemester, today);
        Post post = Post.builder()
                .content(postContent)
                .postType(PostType.LEADERBOARD_HONOR)
                .visibility(PostVisibility.PUBLIC)
                .poster(null) // null = Hệ thống
                .createdAt(now)
                .updatedAt(now)
                .reactCount(0)
                .commentCount(0)
                .build();
        Post savedPost = postRepository.save(post);

        // 3. Đồng bộ tìm kiếm
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.POST, savedPost.getId()));

        // 4. Ghi Activity Feed
        String feedTitle = isEndOfSemester
                ? "🏆 Tổng kết cuối kỳ: Vinh danh Top sinh viên xuất sắc học kỳ " + semester.getName() + "!"
                : "🌟 Vinh danh Top sinh viên dẫn đầu học kỳ " + semester.getName() + " - Tháng " + today.getMonthValue() + "/" + today.getYear() + "!";

        ActivityFeedMetaData feedMeta = ActivityFeedMetaData.builder()
                .title(feedTitle)
                .description(postContent.length() > 200 ? postContent.substring(0, 200) + "..." : postContent)
                .build();
        activityFeedService.logActivity(null, ActionType.CREATED_POST, ActivityFeedTargetType.POST.name(), savedPost.getId(), feedMeta);

        // 5. Gửi thông báo In-app và Push Notification cho các sinh viên đạt giải
        List<Notification> notifs = new ArrayList<>();
        for (LeaderboardWinnerEntry w : winners) {
            String notifTitle = isEndOfSemester
                    ? "🏆 Vinh danh chung cuộc học kỳ " + semester.getName() + "!"
                    : "🌟 Vinh danh Top Bảng xếp hạng tháng " + today.getMonthValue() + "/" + today.getYear() + "!";

            String notifBody = getNotifBody(semester, isEndOfSemester, w);

            notifs.add(Notification.builder()
                    .receiver(userRepository.getReferenceById(w.userId()))
                    .actor(null)
                    .type(NotificationType.LEADERBOARD_HONOR)
                    .category(NotificationCategory.GAMIFICATION)
                    .title(notifTitle)
                    .body(notifBody)
                    .targetType(NotificationTargetType.POST)
                    .targetId(savedPost.getId())
                    .creadtedAt(now)
                    .build());

            eventPublisher.publishEvent(PushNotificationEvent.single(
                    w.userId(),
                    notifTitle,
                    notifBody,
                    null,
                    NotificationTargetType.POST,
                    savedPost.getId(),
                    null,
                    PushNotificationType.GAMIFICATION
            ));
        }

        if (!notifs.isEmpty()) {
            notificationRepository.saveAll(notifs);
        }

        log.info("[HonorPost] Đã tạo thành công bài post vinh danh (ID: {}) cho học kỳ {}", savedPost.getId(), semester.getName());
    }

    private @NonNull String getNotifBody(Semester semester, boolean isEndOfSemester, LeaderboardWinnerEntry w) {
        String rankTitle = (w.rank() == 1) ? (isEndOfSemester ? "Thủ khoa" : "Hạng 1") : "Hạng " + w.rank();
        return isEndOfSemester
                ? "Chúc mừng bạn đã xuất sắc đạt " + rankTitle + " chung cuộc học kỳ " + semester.getName()
                        + " với " + w.points() + " điểm! Bạn nhận được phần thưởng +" + (long) w.coins() + " Xu 🪙."
                : "Chúc mừng bạn đang đạt " + rankTitle + " Bảng xếp hạng học kỳ " + semester.getName()
                        + " với " + w.points() + " điểm! Bạn nhận được phần thưởng +" + (long) w.coins() + " Xu 🪙.";
    }

    private double calculateHonorCoins(int rank, boolean isEndOfSemester) {
        if (isEndOfSemester) {
            return switch (rank) {
                case 1 -> 300.0;
                case 2 -> 180.0;
                case 3 -> 90.0;
                default -> 0.0;
            };
        } else {
            return switch (rank) {
                case 1 -> 30.0;
                case 2 -> 20.0;
                case 3 -> 10.0;
                default -> 0.0;
            };
        }
    }

    private String buildHonorPostContent(Semester semester, List<LeaderboardWinnerEntry> winners,
                                         Map<Long, UserProfile> profileMap, boolean isEndOfSemester,
                                         LocalDate today) {
        StringBuilder sb = new StringBuilder();
        String semesterName = semester.getName() != null ? semester.getName() : "Học kỳ";

        if (isEndOfSemester) {
            sb.append("🏆 [VINH DANH CHUNG CUỘC - BẢNG XẾP HẠNG ").append(semesterName.toUpperCase()).append("] 🏆\n\n");
            sb.append("Học kỳ ").append(semesterName).append(" đã chính thức khép lại thành công tốt đẹp! ")
              .append("Trải qua một kỳ học đầy nỗ lực, cống hiến những câu hỏi giá trị và tham gia sôi nổi vào các hoạt động học thuật, ")
              .append("Ban Quản trị xin long trọng vinh danh các gương mặt tiêu biểu nhất chung cuộc:\n\n");
        } else {
            sb.append("🌟 [VINH DANH TOP BẢNG XẾP HẠNG HỌC KỲ - THÁNG ").append(today.getMonthValue()).append("/").append(today.getYear()).append("] 🌟\n\n");
            sb.append("Chào tháng mới! Ban Quản trị xin trân trọng biểu dương các sinh viên xuất sắc đang dẫn đầu ")
              .append("Bảng xếp hạng học kỳ ").append(semesterName).append(" tính đến thời điểm hiện tại:\n\n");
        }

        for (LeaderboardWinnerEntry w : winners) {
            UserProfile p = profileMap.get(w.userId());
            String name = p != null && p.getFullName() != null ? p.getFullName() : anonymizerUtil.encodeUserId(w.userId());
            String codeStr = p != null && p.getStudentLecturerCode() != null ? " (MSSV: " + p.getStudentLecturerCode() + ")" : "";

            String medal = switch (w.rank()) {
                case 1 -> isEndOfSemester ? "👑 THỦ KHOA HỌC KỲ" : "🥇 Hạng 1";
                case 2 -> isEndOfSemester ? "🥈 Á QUÂN 1" : "🥈 Hạng 2";
                case 3 -> isEndOfSemester ? "🥉 Á QUÂN 2" : "🥉 Hạng 3";
                default -> "🎖️ Hạng " + w.rank();
            };

            sb.append(medal).append(": ").append(name).append(codeStr)
              .append(" — ⭐ ").append(w.points()).append(" điểm")
              .append(" 🎁 Thưởng: +").append((long) w.coins()).append(" Xu 🪙\n");
        }

        sb.append("\n");
        if (isEndOfSemester) {
            sb.append("🎓 Huy hiệu vinh danh danh dự cùng phần thưởng Xu đã được trao tặng vào tài khoản của các bạn. ")
              .append("Cảm ơn toàn thể sinh viên và giảng viên đã luôn đồng hành cùng Ngân hàng câu hỏi trong suốt học kỳ vừa qua. ")
              .append("Chúc các bạn luôn giữ vững phong độ và tiếp tục tỏa sáng ở những học kỳ tiếp theo!\n\n")
              .append("#TongKetHocKy #VinhDanhThuKhoa #TopSinhVienXuatSac");
        } else {
            sb.append("🎉 Phần thưởng Xu đã được chuyển trực tiếp vào ví cá nhân của các bạn! ")
              .append("Chúc toàn thể các bạn sinh viên một tháng mới tràn đầy năng lượng và tích cực tham gia đóng góp những câu hỏi trắc nghiệm chất lượng vào Ngân hàng câu hỏi của Nhà trường!\n\n")
              .append("#VinhDanh #BangXepHang #SinhVienTieuBieu");
        }

        return sb.toString();
    }
}
