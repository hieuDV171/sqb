package com.frozenheart.backend.modules.gamification.service.impl;

import com.frozenheart.backend.core.constant.Point;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.prediction.*;
import com.frozenheart.backend.core.entity.session.*;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.user.User;
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
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.stream.Collectors;

import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.core.entity.user.UserProfile;

import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.modules.session.component.CurrentSemesterHolder;
import com.frozenheart.backend.modules.ai.client.LocalAiClient;
import com.frozenheart.backend.modules.ai.dto.AiRefineResponse;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import java.time.DayOfWeek;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
    private final CounterMetricsService counterMetricsService;
    private final AnonymizerUtil anonymizerUtil;
    private final CurrentSemesterHolder currentSemesterHolder;
    private final RedisTemplate<String, String> redisTemplate;
    private final LeaderboardService leaderboardService;
    private final SemesterRepository semesterRepository;
    private final LocalAiClient localAiClient;
    private final JsonMapper jsonMapper;

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
    // GAME 1: Đoán số lượng người tham gia đề xuất ngày mai (Cutoff: 22:00 hôm nay)
    // =========================================================================
    @Override
    @Transactional
    public GamePredictionResponse predictGame1(Game1PredictionRequest request) {
        LocalTime nowTime = LocalTime.now();
        Long userId = JwtPayload.getCurrentUserPayload().getUserId();

        if (nowTime.isAfter(LocalTime.of(22, 0))) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED,
                    "Hết giờ! Nước mắt em rơi, trò chơi kết thúc");
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
        prediction.setTargetType("SESSION");
        prediction.setTargetId(request.sessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

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
        prediction.setTargetType("SUBJECT");
        prediction.setTargetId(request.subjectId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

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
    public Game6ActiveSessionResponse processAndGenerateGame6WeeklySession() {
        LocalDate today = LocalDate.now();
        if (today.getDayOfWeek() != DayOfWeek.SATURDAY) {
            log.warn("[Game 6 Cron] Today is {} (not Saturday). Skipping Game 6 process.", today.getDayOfWeek());
            throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Game 6 chỉ được xử lý và mở phiên vào ngày Thứ 7");
        }

        // TỔNG KẾT PHIÊN CŨ (NẾU CÓ ACTIVE SESSION)
        Optional<MinigameLlmSession> activeSessionOpt = minigameLlmSessionRepository
                .findFirstByStatusOrderByStartTimeDesc(MinigameLlmSessionStatus.ACTIVE);

        if (activeSessionOpt.isPresent()) {
            MinigameLlmSession activeSession = activeSessionOpt.get();

            Map<String, Boolean> correctAnswersMap = activeSession.getCorrectAnswers();
            Set<Long> actualLlmQuestionIds = new HashSet<>();
            if (correctAnswersMap != null) {
                correctAnswersMap.forEach((qIdStr, isLlm) -> {
                    if (Boolean.TRUE.equals(isLlm)) {
                        try {
                            actualLlmQuestionIds.add(Long.parseLong(qIdStr));
                        } catch (NumberFormatException ignored) {}
                    }
                });
            }

            List<Prediction> predictions = predictionRepository.findByGameTypeAndTargetTypeAndTargetIdFetchGambler(
                    GameType.GAME_6_LLM_IDENTIFICATION, "GAME6_SESSION", activeSession.getId());

            for (Prediction pred : predictions) {
                pred.setStatus(PredictionStatus.RESOLVED);
                pred.setResolvedAt(LocalDateTime.now());

                boolean isCorrect = false;
                if (pred.getPredictionData() != null && pred.getPredictionData().getData() instanceof Map<?, ?> dataMap) {
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
                        "GAME_6_WIN",
                        "GAME6_SESSION",
                        activeSession.getId(),
                        null
                    );
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
            candidates.add(new CandidateQuestion(q.getId(), BankType.CURRENT, q.getContent(), q.getOptions(), q.getExplanation(), mediaUrls));
        }
        for (LegacyQuestion lq : legacyQuestions) {
            candidates.add(new CandidateQuestion(lq.getId(), BankType.LEGACY, lq.getContent(), lq.getOptions(), lq.getExplanation(), lq.getImageUrls()));
        }

        Random random = new Random();
        int k = random.nextInt(candidates.size() + 1); // 0..7
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        Set<Integer> aiIndices = new HashSet<>(indices.subList(0, k));

        LocalDateTime startTime = today.atStartOfDay();
        LocalDateTime endTime = today.atTime(23, 59, 59);

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
                AiRefineResponse.SuggestedQuestionDto aiResult = refineQuestionWithAi(cand.content(), cand.options(), cand.explanation());
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
                    .createdAt(LocalDateTime.now())
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
                        .id(q.getId())
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

    private AiRefineResponse.SuggestedQuestionDto refineQuestionWithAi(String content, List<QuestionOption> options, String explanation) {
        try {
            String optsJson = jsonMapper.writeValueAsString(options != null ? options : List.of());
            String userPrompt = String.format("""
                    Nội dung câu hỏi: %s
                    Các phương án: %s
                    Lời giải: %s
                    Yêu cầu: Câu hỏi sẽ được dùng vào cuộc thi dự đoán xem câu hỏi nào do AI tạo.
                     Bạn hãy chỉnh sửa theo ý bạn sao cho khó đoán ra nhất có thể
                    """, content != null ? content : "", optsJson, explanation != null ? explanation : "");

            LocalAiClient.LocalAiResponse aiResponse = localAiClient.generateCompletion(GAME6_SYSTEM_PROMPT, userPrompt);
            String rawResponse = aiResponse.content();
            if (rawResponse == null) return null;

            Pattern pattern = Pattern.compile("<JSON>(.*?)</JSON>", Pattern.DOTALL);
            Matcher matcher = pattern.matcher(rawResponse);
            String jsonText = rawResponse;
            if (matcher.find()) {
                jsonText = matcher.group(1).trim();
            }

            Map<String, Object> map = jsonMapper.readValue(jsonText, new TypeReference<>() {});
            String refinedContent = (String) map.get("content");
            String refinedExplanation = (String) map.get("explanation");

            List<QuestionOption> refinedOptions = null;
            if (map.get("options") instanceof List<?> rawOpts) {
                refinedOptions = jsonMapper.convertValue(rawOpts, new TypeReference<>() {});
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
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED, "Phiên trò chơi hiện không trong trạng thái mở");
        }

        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(session.getStartTime()) || now.isAfter(session.getEndTime())) {
            throw new AppException(ResponseCode.PREDICTION_WINDOW_CLOSED, "Hết giờ! Nước mắt em rơi, trò chơi kết thúc");
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
        prediction.setTargetType("GAME6_SESSION");
        prediction.setTargetId(request.minigameSessionId());
        prediction.setStatus(PredictionStatus.PENDING);
        prediction.setCreatedAt(LocalDateTime.now());

        Map<String, Object> predData = Map.of("selectedLlmQuestionIds", request.selectedLlmQuestionIds());
        return getGamePredictionResponse(prediction, predData);
    }

    private GamePredictionResponse getGamePredictionResponse(Prediction prediction, Map<String, Object> predData) {
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
                        "GAME_3_AUTHOR_CONFIDENCE_WIN",
                        "SESSION",
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
        LocalDate yesterday = LocalDate.now().minusDays(1);
        LocalDateTime startOfYesterday = yesterday.atStartOfDay();
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();

        List<Prediction> preds = predictionRepository.findByGameTypeAndTargetDateBetweenFetchGambler(
                GameType.GAME_1_PARTICIPANTS,
                startOfYesterday,
                startOfToday.minusNanos(1));

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
                            .importedAt(LocalDateTime.now())
                            .build();
                })
                .toList();

        legacyQuestionRepository.saveAll(legacyToSave);

        // Reset toàn bộ is_used = false trong legacy_questions cho học kỳ mới
        legacyQuestionRepository.resetAllIsUsedToFalse();

        Semester current = currentSemesterHolder.getCurrentSemester();
        current.setFinalized(true);
        semesterRepository.save(current);

        leaderboardService.scheduleOldSemesterCleanup(currentSemesterHolder.getCurrentSemester().getId());

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

    // TODO: Khi FE gọi, phải bảo FE kiểm tra, nếu rank không tuần tự, gợi ý người dùng load lại từ trang đầu.
    // VD: Người dùng lấy từ 1-20, 10 giây sau có 5 người nhảy vào top 10. Khi người
    // dùng gọi trang 2 -> sẽ bị lấy từ 26-45, nếu FE phát hiện không liên tiếp, phải làm nút refresh
    // động cho phép người dùng muốn làm mới ngay
    // =========================================================================
    // GET LEADERBOARD
    // =========================================================================
    @Override
    @Transactional(readOnly = true)
    public LeaderboardResponse getLeaderboard(LeaderboardPeriod period, Long subjectId, Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        int pageLimit = Math.clamp(limit != null ? limit : 20, 1, 30);

        String redisKey = RedisKeyUtil.buildLeaderboardKey(period, currentSemesterHolder.getCurrentSemester().getId(), subjectId);
        ZSetOperations<String, String> zSetOps = redisTemplate.opsForZSet();

        Long totalParticipants = zSetOps.zCard(redisKey);
        if (totalParticipants == null) totalParticipants = 0L;

        MyRankDto myRankDto = buildMyRankDto(currentUserId, redisKey, totalParticipants, zSetOps);

        if (totalParticipants == 0) {
            return buildEmptyResponse(myRankDto);
        }

        long startIndex = 0;

        // Nếu có truyền cursor (userId của người cuối trang trước)
        if (after != null) {
            // Tìm rank HIỆN TẠI của người đó (O(log N))
            Long cursorRank = zSetOps.reverseRank(redisKey, String.valueOf(after));

            if (cursorRank != null) {
                // Trang tiếp theo bắt đầu từ người ngay phía sau cursor
                startIndex = cursorRank + 1;
            } else {
                // Trường hợp user làm cursor đã bị xóa khỏi Redis, trả về rỗng để an toàn
                return buildEmptyResponse(myRankDto);
            }
        }

        // Lấy thừa 1 phần tử để xác định hasNext
        long endIndex = startIndex + pageLimit;
        Set<ZSetOperations.TypedTuple<String>> rawRedisList = zSetOps.reverseRangeWithScores(redisKey, startIndex, endIndex);

        if (rawRedisList == null || rawRedisList.isEmpty()) {
            return buildEmptyResponse(myRankDto);
        }

        // ==========================================
        // XỬ LÝ DỮ LIỆU TRẢ VỀ & TÍNH CURSOR TIẾP THEO
        // ==========================================
        boolean hasNext = rawRedisList.size() > pageLimit;

        // Chỉ lấy đúng số lượng pageLimit để xử lý
        List<ZSetOperations.TypedTuple<String>> pagedRaw = rawRedisList.stream()
                .limit(pageLimit)
                .toList();

        Set<Long> pagedUserIds = pagedRaw.stream()
                .map(tuple -> tuple.getValue() != null ? Long.parseLong(tuple.getValue()) : 0)
                .collect(Collectors.toSet());

        Map<Long, UserProfile> profileMap = userProfileRepository.findAllById(pagedUserIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<LeaderboardEntryDto> entries = new ArrayList<>();
        long currentRank = startIndex + 1;
        Long nextCursorUserId = null;

        for (ZSetOperations.TypedTuple<String> tuple : pagedRaw) {
            Long uId = 0L;
            if (tuple.getValue() == null) {
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

            // Cập nhật liên tục, đến cuối vòng lặp sẽ lưu được userId của người cuối cùng
            nextCursorUserId = uId;
        }

        CursorPaginationDto pagination = CursorPaginationDto.builder()
                .after(hasNext ? nextCursorUserId : null)
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
                .pagination(CursorPaginationDto.builder().after(null).hasNext(false).build())
                .build();
    }

    private MyRankDto buildMyRankDto(Long userId, String redisKey, Long totalParticipants, ZSetOperations<String, String> zSetOps) {
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

}
