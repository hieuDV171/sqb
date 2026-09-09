package com.frozenheart.backend.modules.session.service.impl;

import java.time.Instant;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.media.MediaTarget;
import com.frozenheart.backend.core.entity.media.QuestionMedia;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.session.QuestionOption;
import com.frozenheart.backend.core.entity.session.QuestionSource;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswerId;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionRequest;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionResponse;
import com.frozenheart.backend.modules.session.dto.QuestionRatingsResponse;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.RateQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RateQuestionResponse;
import com.frozenheart.backend.modules.session.dto.UserQuestionsResponse;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.UserAnswerRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.session.service.QuestionInteractionService;
import com.frozenheart.backend.modules.user.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionInteractionServiceImpl implements QuestionInteractionService {

        private final QuestionRepository questionRepository;
        private final UserAnswerRepository userAnswerRepository;
        private final UserRatingRepository userRatingRepository;
        private final UserRepository userRepository;

        @Override
        @Transactional(readOnly = true)
        public UserQuestionsResponse getUserProposedQuestions(Long userId, Long after, Integer limit, Long subjectId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                List<Question> questions = questionRepository.findUserQuestionsWithCursor(userId,
                                SessionStatus.RESOLVED, subjectId, after, pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (questions.size() > pageSize) {
                        hasNext = true;
                        questions = questions.subList(0, pageSize);
                        nextCursor = questions.getLast().getId();
                }

                List<Long> questionIds = questions.stream().map(Question::getId).toList();

                final Set<Long> answeredQIds = questionIds.isEmpty() ? Set.of()
                                : userAnswerRepository.findByUserIdAndQuestionIdIn(currentUserId, questionIds).stream()
                                                .map(a -> a.getQuestion().getId())
                                                .collect(Collectors.toSet());

                final Set<Long> ratedQIds = questionIds.isEmpty() ? Set.of()
                                : userRatingRepository.findByUserIdAndRatedQuestionIdIn(currentUserId, questionIds)
                                                .stream()
                                                .map(r -> r.getRatedQuestion().getId())
                                                .collect(Collectors.toSet());

                List<UserQuestionsResponse.UserQuestionItemDto> items = questions.stream().map(q -> {
                        List<String> imageUrls = q.getOwnedMedias() != null ? q.getOwnedMedias().stream()
                                        .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                                        .map(QuestionMedia::getUrl)
                                        .toList() : List.of();

                        boolean answered = answeredQIds.contains(q.getId());
                        boolean rated = ratedQIds.contains(q.getId());

                        UserQuestionsResponse.MyInteractionDto myInteraction = UserQuestionsResponse.MyInteractionDto
                                        .builder()
                                        .answered(answered)
                                        .rated(rated)
                                        .build();

                        List<String> correctKeys = q.getOptions() != null ? q.getOptions().stream()
                                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                                        .map(QuestionOption::getKey)
                                        .toList() : List.of();

                        UserQuestionsResponse.HiddenFieldsDto hiddenFields = answered
                                        ? UserQuestionsResponse.HiddenFieldsDto.builder()
                                                        .correctAnswer(String.join(", ", correctKeys))
                                                        .explanation(q.getExplanation())
                                                        .build()
                                        : null;

                        List<QuestionOption> sanitizedOptions = q.getOptions() != null
                                        ? q.getOptions().stream().map(o -> {
                                                QuestionOption opt = new QuestionOption();
                                                opt.setKey(o.getKey());
                                                opt.setText(o.getText());
                                                opt.setMediaUrl(o.getMediaUrl());
                                                opt.setMediaId(o.getMediaId());
                                                opt.setIsCorrect(null);
                                                return opt;
                                        }).toList()
                                        : List.of();

                        return UserQuestionsResponse.UserQuestionItemDto.builder()
                                        .questionId(q.getId())
                                        .questionCode(q.getQuestionCode())
                                        .content(q.getContent())
                                        .imageUrls(imageUrls)
                                        .options(sanitizedOptions)
                                        .source(q.isLlmGenerated() ? QuestionSource.LLM : QuestionSource.HOMO_SAPIENS)
                                        .createdAt(q.getCreatedAt())
                                        .updatedAt(q.getUpdatedAt() != null ? q.getUpdatedAt() : q.getCreatedAt())
                                        .myInteraction(myInteraction)
                                        .hiddenFields(hiddenFields)
                                        .reactCount(q.getReactCount() != null ? q.getReactCount() : 0)
                                        .commentCount(q.getCommentCount() != null ? q.getCommentCount() : 0)
                                        .ratingCount(q.getRatingCount() != null ? q.getRatingCount() : 0)
                                        .build();
                }).toList();

                return UserQuestionsResponse.builder()
                                .items(items)
                                .pagination(CursorPaginationDto.builder()
                                                .after(nextCursor)
                                                .hasNext(hasNext)
                                                .build())
                                .build();
        }

        @Override
        @Transactional
        public AnswerQuestionResponse answerQuestion(Long questionId, AnswerQuestionRequest request) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                if (userAnswerRepository.existsByIdUserIdAndIdQuestionId(currentUserId, questionId)) {
                        throw new AppException(ResponseCode.QUESTION_ALREADY_ANSWERED);
                }

                Question question = questionRepository.findById(questionId)
                                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));

                Instant currentUpdatedAt = question.getUpdatedAt() != null ? question.getUpdatedAt()
                                : question.getCreatedAt();
                if (request.questionUpdatedAt() != null && currentUpdatedAt != null
                                && !currentUpdatedAt.equals(request.questionUpdatedAt())) {
                        throw new AppException(ResponseCode.STALE_DATA_DETECTED,
                                        "Dữ liệu câu hỏi đã được cập nhật mới. Vui lòng tải lại câu hỏi để thực hiện lại!");
                }

                Set<String> correctKeys = question.getOptions() != null ? question.getOptions().stream()
                                .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                                .map(QuestionOption::getKey)
                                .collect(Collectors.toSet()) : Set.of();

                Set<String> selectedKeys = new HashSet<>(request.selectedOptions());
                boolean isCorrect = !correctKeys.isEmpty() && correctKeys.equals(selectedKeys);

                List<QuestionOption> selectedOptions = question.getOptions() != null
                                ? question.getOptions().stream()
                                                .filter(o -> selectedKeys.contains(o.getKey()))
                                                .toList()
                                : List.of();

                User userRef = userRepository.getReferenceById(currentUserId);
                UserAnswerId answerId = new UserAnswerId(currentUserId, questionId);

                UserAnswer userAnswer = UserAnswer.builder()
                                .id(answerId)
                                .user(userRef)
                                .question(question)
                                .selectedOptions(selectedOptions)
                                .isCorrect(isCorrect)
                                .createdAt(Instant.now())
                                .build();

                userAnswerRepository.save(userAnswer);

                String correctAnswerStr = String.join(", ", correctKeys);

                return AnswerQuestionResponse.builder()
                                .isCorrect(isCorrect)
                                .correctAnswer(correctAnswerStr)
                                .explanation(question.getExplanation())
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public QuestionStatisticsResponse getQuestionStatistics(Long questionId) {
                questionRepository.findById(questionId)
                                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));

                Object[] answerStatsRow = userAnswerRepository.getAnswerStatsSummary(questionId);
                long totalAnswer = answerStatsRow != null && answerStatsRow.length > 0 && answerStatsRow[0] != null
                                ? ((Number) answerStatsRow[0]).longValue()
                                : 0L;
                long correctCount = answerStatsRow != null && answerStatsRow.length > 1 && answerStatsRow[1] != null
                                ? ((Number) answerStatsRow[1]).longValue()
                                : 0L;
                double correctRate = totalAnswer > 0 ? (double) correctCount / totalAnswer : 0.0;

                List<List<QuestionOption>> selectedOptionsLists = userAnswerRepository
                                .findSelectedOptionsByQuestionId(questionId);
                Map<String, Long> optionDistribution = new HashMap<>();
                if (selectedOptionsLists != null) {
                        for (List<QuestionOption> list : selectedOptionsLists) {
                                if (list != null) {
                                        for (QuestionOption opt : list) {
                                                if (opt.getKey() != null) {
                                                        optionDistribution.merge(opt.getKey(), 1L, Long::sum);
                                                }
                                        }
                                }
                        }
                }

                Object[] ratingStatsRow = userRatingRepository.getRatingStatsSummary(questionId);
                double avgRating = ratingStatsRow != null && ratingStatsRow.length > 0 && ratingStatsRow[0] != null
                                ? ((Number) ratingStatsRow[0]).doubleValue()
                                : 0.0;
                long totalRatings = ratingStatsRow != null && ratingStatsRow.length > 1 && ratingStatsRow[1] != null
                                ? ((Number) ratingStatsRow[1]).longValue()
                                : 0L;

                Map<String, Long> ratingDist = new HashMap<>();
                for (int i = 0; i <= 4; i++) {
                        long count = (ratingStatsRow != null && ratingStatsRow.length > (i + 2)
                                        && ratingStatsRow[i + 2] != null) ? ((Number) ratingStatsRow[i + 2]).longValue()
                                                        : 0L;
                        ratingDist.put(String.valueOf(i), count);
                }

                return QuestionStatisticsResponse.builder()
                                .totalAnswer(totalAnswer)
                                .correctRate(correctRate)
                                .optionDistribution(optionDistribution)
                                .ratingSummary(QuestionStatisticsResponse.RatingSummaryDto.builder()
                                                .avgRating(avgRating)
                                                .totalRatings(totalRatings)
                                                .distribution(ratingDist)
                                                .build())
                                .build();
        }

        @Override
        @Transactional
        public RateQuestionResponse rateQuestion(Long questionId, RateQuestionRequest request) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                Question question = questionRepository.findById(questionId)
                                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));

                User userRef = userRepository.getReferenceById(currentUserId);
                UserRatingId ratingId = new UserRatingId(currentUserId, questionId);

                boolean isError = request.isError() != null && request.isError();
                double rating = isError ? 0 : request.rating().doubleValue();

                UserRating userRating = UserRating.builder()
                                .id(ratingId)
                                .user(userRef)
                                .ratedQuestion(question)
                                .rating(rating)
                                .isError(isError)
                                .comment(request.comment())
                                .createdAt(Instant.now())
                                .build();

                userRatingRepository.save(userRating);

                Object[] ratingRow = userRatingRepository.getAvgAndCountByQuestionId(questionId);
                double newAvgRating = (ratingRow != null && ratingRow.length > 0 && ratingRow[0] != null)
                                ? ((Number) ratingRow[0]).doubleValue()
                                : 0.0;
                long newCount = (ratingRow != null && ratingRow.length > 1 && ratingRow[1] != null)
                                ? ((Number) ratingRow[1]).longValue()
                                : 0L;

                question.setRatingCount((int) newCount);

                // Hoạt động thường xuyên, không đánh index
                questionRepository.save(question);

                return RateQuestionResponse.builder()
                                .newAvgRating(newAvgRating)
                                .newRatingCount((int) newCount)
                                .build();
        }

        @Override
        @Transactional(readOnly = true)
        public QuestionRatingsResponse getQuestionRatings(Long questionId, Long after, Integer limit) {
                questionRepository.findById(questionId)
                                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));

                int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
                Pageable pageable = PageRequest.of(0, pageSize + 1);

                List<UserRating> ratings = userRatingRepository.findRatingsByQuestionIdWithCursor(questionId, after,
                                pageable);

                boolean hasNext = false;
                Long nextCursor = null;

                if (ratings.size() > pageSize) {
                        hasNext = true;
                        ratings = ratings.subList(0, pageSize);
                        nextCursor = ratings.getLast().getId().getUserId();
                }

                List<QuestionRatingsResponse.RatingItemDto> items = ratings.stream()
                                .map(r -> QuestionRatingsResponse.RatingItemDto.builder()
                                                .userId(r.getId().getUserId())
                                                .rating(r.getRating())
                                                .isError(r.isError())
                                                .comment(r.getComment())
                                                .createdAt(r.getCreatedAt())
                                                .build())
                                .toList();

                return QuestionRatingsResponse.builder()
                                .items(items)
                                .pagination(CursorPaginationDto.builder()
                                                .after(nextCursor)
                                                .hasNext(hasNext)
                                                .build())
                                .build();
        }

}
