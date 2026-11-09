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
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.core.entity.socialinteraction.QuestionErrorReport;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswer;
import com.frozenheart.backend.core.entity.socialinteraction.UserAnswerId;
import com.frozenheart.backend.core.entity.socialinteraction.UserRating;
import com.frozenheart.backend.core.entity.socialinteraction.UserRatingId;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionRequest;
import com.frozenheart.backend.modules.session.dto.AnswerQuestionResponse;
import com.frozenheart.backend.modules.session.dto.QuestionRatingsResponse;
import com.frozenheart.backend.modules.session.dto.QuestionStatisticsResponse;
import com.frozenheart.backend.modules.session.dto.AnswerStatisticsProjection;
import com.frozenheart.backend.modules.session.dto.RatingAverageProjection;
import com.frozenheart.backend.modules.session.dto.RatingStatisticsProjection;
import com.frozenheart.backend.modules.session.dto.RateQuestionRequest;
import com.frozenheart.backend.modules.session.dto.RateQuestionResponse;
import com.frozenheart.backend.modules.session.dto.SessionQuestionsResponse;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.QuestionErrorReportRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.repository.UserAnswerRepository;
import com.frozenheart.backend.modules.session.repository.UserRatingRepository;
import com.frozenheart.backend.modules.session.service.QuestionInteractionService;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class QuestionInteractionServiceImpl implements QuestionInteractionService {

        private final QuestionRepository questionRepository;
        private final SessionRepository sessionRepository;
        private final UserAnswerRepository userAnswerRepository;
        private final UserRatingRepository userRatingRepository;
        private final UserRepository userRepository;
        private final UserProfileRepository userProfileRepository;
        private final ReactRepository reactRepository;
        private final QuestionErrorReportRepository errorReportRepository;

        @Override
        @Transactional(readOnly = true)
        public SessionQuestionsResponse getSessionQuestions(Long sessionId) {
                Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

                Session session = sessionRepository.findByIdFetchCourseClass(sessionId)
                                .orElseThrow(() -> new AppException(ResponseCode.SESSION_NOT_FOUND));

                if (!session.isPracticeVisibleTo(currentUserId)) {
                        throw new AppException(ResponseCode.ACCESS_DENIED, "Phiên đề xuất chưa được phê duyệt");
                }

                List<Question> questions = questionRepository.findQuestionsBySessionId(sessionId);
                List<SessionQuestionsResponse.PracticeQuestionDto> questionDtos = mapQuestionsToDtos(questions, currentUserId);

                Subject subject = session.getCourseClass().getSubject();

                return SessionQuestionsResponse.builder()
                                .sessionId(session.getId())
                                .sessionCode(session.getSessionCode())
                                .title(session.getTitle())
                                .content(session.getContent())
                                .subjectId(subject != null ? subject.getId() : null)
                                .subjectName(subject != null ? subject.getName() : null)
                                .subjectCode(subject != null ? subject.getCode() : null)
                                .createdAt(session.getCreatedAt())
                                .totalQuestions(questions.size())
                                .questions(questionDtos)
                                .build();
        }

        private List<SessionQuestionsResponse.PracticeQuestionDto> mapQuestionsToDtos(List<Question> questions, Long currentUserId) {
                List<Long> questionIds = questions.stream().map(Question::getId).toList();

                if (questionIds.isEmpty()) return List.of();
                var answers = userAnswerRepository.findByUserIdAndQuestionIdIn(currentUserId, questionIds).stream()
                                .collect(Collectors.toMap(a -> a.getId().getQuestionId(), a -> a));
                var ratings = userRatingRepository.findByUserIdAndRatedQuestionIdIn(currentUserId, questionIds).stream()
                                .collect(Collectors.toMap(r -> r.getId().getRatedQuestionId(), r -> r));
                var reported = Set.copyOf(errorReportRepository.findReportedQuestionIds(currentUserId, questionIds));
                var ratingCounts = userRatingRepository.countStarsByQuestionIds(questionIds).stream()
                                .collect(Collectors.toMap(UserRatingRepository.QuestionRatingCount::getQuestionId,
                                                UserRatingRepository.QuestionRatingCount::getRatingCount));
                final Map<Long, ReactionType> reactions =
                                reactRepository.findUserReactsByTargetIds(currentUserId, InteractionTargetType.QUESTION, questionIds)
                                                .stream()
                                                .collect(Collectors.toMap(React::getTargetId, React::getReactionType, (first, _) -> first));

                return questions.stream().map(q -> {
                        List<String> imageUrls = q.getOwnedMedias() != null ? q.getOwnedMedias().stream()
                                        .filter(m -> m.getMediaTarget() == MediaTarget.CONTENT)
                                        .map(QuestionMedia::getUrl)
                                        .toList() : List.of();

                        UserAnswer answer = answers.get(q.getId());
                        UserRating feedback = ratings.get(q.getId());
                        boolean rated = feedback != null;

                        SessionQuestionsResponse.MyInteractionDto myInteraction = SessionQuestionsResponse.MyInteractionDto
                                        .builder()
                                        .answered(answer != null)
                                        .rated(rated)
                                        .myReaction(reactions.get(q.getId()))
                                        .myRating(rated ? feedback.getRating() : null)
                                        .ratingComment(rated ? feedback.getComment() : null)
                                        .reported(reported.contains(q.getId()))
                                        .build();

                        List<String> correctKeys = q.getOptions() != null ? q.getOptions().stream()
                                        .filter(o -> Boolean.TRUE.equals(o.getIsCorrect()))
                                        .map(QuestionOption::getKey)
                                        .toList() : List.of();

                        SessionQuestionsResponse.HiddenFieldsDto hiddenFields = answer != null
                                        ? SessionQuestionsResponse.HiddenFieldsDto.builder()
                                                        .correctAnswer(String.join(", ", correctKeys))
                                                        .explanation(q.getExplanation())
                                                        .selectedOptions(answer.getSelectedOptions() == null ? List.of() :
                                                                        answer.getSelectedOptions().stream()
                                                                                        .map(option -> new SessionQuestionsResponse.SubmittedOptionDto(option.getKey(), option.getText()))
                                                                                        .toList())
                                                        .wasCorrect(answer.isCorrect())
                                                        .questionUpdatedSinceAnswer(q.getUpdatedAt() != null && answer.getCreatedAt() != null
                                                                        && q.getUpdatedAt().isAfter(answer.getCreatedAt()))
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

                        return SessionQuestionsResponse.PracticeQuestionDto.builder()
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
                                        .ratingCount(ratingCounts.getOrDefault(q.getId(), 0L).intValue())
                                        .build();
                }).toList();
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

                AnswerStatisticsProjection answerStats = userAnswerRepository.getAnswerStatsSummary(questionId);
                long totalAnswer = answerStats != null && answerStats.getTotalAnswers() != null
                                ? answerStats.getTotalAnswers()
                                : 0L;
                long correctCount = answerStats != null && answerStats.getCorrectAnswers() != null
                                ? answerStats.getCorrectAnswers()
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

                RatingStatisticsProjection ratingStats = userRatingRepository.getRatingStatsSummary(questionId);
                double avgRating = ratingStats != null && ratingStats.getAvgRating() != null
                                ? ratingStats.getAvgRating()
                                : 0.0;
                long totalRatings = ratingStats != null && ratingStats.getTotalRatings() != null
                                ? ratingStats.getTotalRatings()
                                : 0L;

                Map<String, Long> ratingDist = new HashMap<>();
                ratingDist.put("0", getRatingCount(ratingStats, 0));
                ratingDist.put("1", getRatingCount(ratingStats, 1));
                ratingDist.put("2", getRatingCount(ratingStats, 2));
                ratingDist.put("3", getRatingCount(ratingStats, 3));
                ratingDist.put("4", getRatingCount(ratingStats, 4));

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
                Question question = lockFeedbackQuestion(questionId);
                User userRef = userRepository.getReferenceById(currentUserId);
                UserRatingId id = new UserRatingId(currentUserId, questionId);
                UserRating rating = userRatingRepository.findById(id)
                                .orElseGet(() -> UserRating.builder().id(id).user(userRef)
                                                .ratedQuestion(question).createdAt(Instant.now()).build());
                rating.setRating(request.rating().doubleValue());
                rating.setComment(request.comment());
                userRatingRepository.save(rating);
                RateQuestionResponse summary = readRatingSummary(questionId);
                question.setAvgRating(summary.newAvgRating());
                question.setRatingCount(summary.newRatingCount());
                return summary;
        }

        @Override
        @Transactional
        public void reportQuestion(Long questionId, String description) {
                Long userId = JwtPayload.getCurrentUserPayload().getUserId();
                Question question = lockFeedbackQuestion(questionId);
                saveReport(new UserRatingId(userId, questionId), userRepository.getReferenceById(userId),
                                question, description.strip());
        }

        private Question lockFeedbackQuestion(Long questionId) {
                Question question = questionRepository.findByIdForFeedback(questionId)
                                .orElseThrow(() -> new AppException(ResponseCode.QUESTION_NOT_FOUND));
                if (question.getSession().getStatus() != SessionStatus.RESOLVED) {
                        throw new AppException(ResponseCode.ACCESS_DENIED);
                }
                return question;
        }

        private void saveReport(UserRatingId id, User user, Question question, String description) {
                QuestionErrorReport report = errorReportRepository.findById(id)
                                .orElseGet(() -> QuestionErrorReport.builder().id(id).reporter(user)
                                                .question(question).createdAt(Instant.now()).build());
                report.setDescription(description);
                errorReportRepository.save(report);
        }

        private RateQuestionResponse readRatingSummary(Long questionId) {
                RatingAverageProjection ratingAverage = userRatingRepository
                                .getAvgAndCountByQuestionId(questionId);
                double newAvgRating = ratingAverage != null && ratingAverage.getAvgRating() != null
                                ? ratingAverage.getAvgRating()
                                : 0.0;
                long newCount = ratingAverage != null && ratingAverage.getTotalRatings() != null
                                ? ratingAverage.getTotalRatings()
                                : 0L;

                return RateQuestionResponse.builder()
                                .newAvgRating(newAvgRating)
                                .newRatingCount((int) newCount)
                                .build();
        }

        private long getRatingCount(RatingStatisticsProjection stats, int rating) {
                if (stats == null) {
                        return 0L;
                }

                Long count = switch (rating) {
                        case 0 -> stats.getRating0Count();
                        case 1 -> stats.getRating1Count();
                        case 2 -> stats.getRating2Count();
                        case 3 -> stats.getRating3Count();
                        case 4 -> stats.getRating4Count();
                        default -> 0L;
                };
                return count != null ? count : 0L;
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

                List<Long> reviewerIds = ratings.stream().map(r -> r.getId().getUserId()).toList();
                Map<Long, String> reviewerNames = reviewerIds.isEmpty() ? Map.of()
                                : userProfileRepository.findAllById(reviewerIds).stream()
                                                .filter(profile -> profile.getFullName() != null)
                                                .collect(Collectors.toMap(UserProfile::getUserId, UserProfile::getFullName));

                List<QuestionRatingsResponse.RatingItemDto> items = ratings.stream()
                                .map(r -> QuestionRatingsResponse.RatingItemDto.builder()
                                                .userId(r.getId().getUserId())
                                                .reviewerName(reviewerNames.get(r.getId().getUserId()))
                                                .rating(r.getRating())
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
