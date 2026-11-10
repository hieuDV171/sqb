package com.frozenheart.backend.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.socialinteraction.Search;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository.SearchPostCounters;
import com.frozenheart.backend.modules.search.document.*;
import com.frozenheart.backend.modules.search.dto.*;
import com.frozenheart.backend.modules.search.repository.SearchRepository;
import com.frozenheart.backend.modules.search.service.SearchService;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository.SearchPracticeTarget;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.service.SessionCodeSearch;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient client;
    private final JsonMapper jsonMapper;
    private final SearchRepository searchRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;
    private final TransactionTemplate transactionTemplate;
    private final PostRepository postRepository;
    private final QuestionRepository questionRepository;
    private final SessionRepository sessionRepository;

    private static final String TRENDING_SEARCHES_KEY_PREFIX = "trending_searches:v2:";
    private static final String TRENDING_SEARCHES_SEEN_KEY_PREFIX = "trending_searches:v2:seen:";
    private static final int TRENDING_WINDOW_DAYS = 7;
    private static final int TRENDING_CANDIDATES_PER_DAY = 50;
    private static final ZoneId SEARCH_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    private static final int MAX_SAVED_SEARCHES_PER_USER = 20;

    @Override
    public GlobalSearchResponseDto search(
            String query,
            SearchType type,
            Long after,
            Integer limit) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        int fromOffset = (after != null && after >= 0) ? after.intValue() : 0;

        if (type == null || type == SearchType.ALL || type == SearchType.SESSION) {
            String code = SessionCodeSearch.normalize(query);
            Long sessionId = SessionCodeSearch.sessionId(code);
            if (sessionId != null) return searchSessionByCode(sessionId, code, fromOffset);
        }

        // Xác định danh sách Index cần truy vấn
        List<String> indices = resolveTargetIndices(type);

        try {
            Query esQuery = buildQuery(query);

            SearchResponse<ObjectNode> response = client.search(s -> s
                    .index(indices)
                    .query(esQuery)
                    .from(fromOffset)
                    .size(pageSize + 1)
                    .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                    ObjectNode.class);

            List<Hit<ObjectNode>> hits = response.hits().hits();
            boolean hasNext = hits.size() > pageSize;
            List<Hit<ObjectNode>> resultHits = hasNext ? hits.subList(0, pageSize) : hits;

            List<GlobalSearchResponseDto.SearchResultItemDto> items = new ArrayList<>();
            for (Hit<ObjectNode> hit : resultHits) {
                GlobalSearchResponseDto.SearchResultItemDto item = mapHitToSearchResultItem(hit);
                if (item != null) {
                    items.add(item);
                }
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : items.size();
            resolveCurrentSearchData(items);
            Long nextCursor = hasNext ? (long) (fromOffset + pageSize) : null;

            return GlobalSearchResponseDto.builder()
                    .items(items)
                    .pagination(CursorPaginationDto.builder()
                            .after(nextCursor)
                            .hasNext(hasNext)
                            .build())
                    .totalHits(totalHits)
                    .build();

        } catch (Exception e) {
            log.error("[SearchService] ❌ Lỗi khi thực hiện tìm kiếm trên Elasticsearch: {}", e.getMessage(), e);
            return GlobalSearchResponseDto.builder()
                    .items(Collections.emptyList())
                    .pagination(CursorPaginationDto.builder().hasNext(false).build())
                    .totalHits(0L)
                    .build();
        }
    }

    private GlobalSearchResponseDto searchSessionByCode(Long sessionId, String code, int offset) {
        List<GlobalSearchResponseDto.SearchResultItemDto> items = List.of();
        if (offset == 0) {
            Long viewerId = JwtPayload.getCurrentUserPayload().getUserId();
            items = sessionRepository.findVisibleSessionByCode(sessionId, code, viewerId)
                    .map(row -> GlobalSearchResponseDto.SearchResultItemDto.builder()
                            .type(SearchType.SESSION)
                            .session(GlobalSearchResponseDto.SessionSearchResultDto.builder()
                                    .sessionId(row.getSessionId())
                                    .sessionCode(row.getSessionCode())
                                    .title(row.getTitle())
                                    .content(row.getContent())
                                    .subjectId(row.getSubjectId())
                                    .subjectName(row.getSubjectName())
                                    .status(row.getStatus())
                                    .questionCount(row.getQuestionCount())
                                    .reactCount(row.getReactCount())
                                    .commentCount(row.getCommentCount())
                                    .createdAt(row.getCreatedAt())
                                    .build())
                            .build())
                    .stream().toList();
        }
        return GlobalSearchResponseDto.builder()
                .items(items)
                .totalHits((long) items.size())
                .pagination(CursorPaginationDto.builder().hasNext(false).build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public SavedAndTrendingSearchesResponseDto getSavedAndTrendingSearches() {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        // 1. Lấy lịch sử tìm kiếm cá nhân từ PostgreSQL
        List<SavedAndTrendingSearchesResponseDto.SavedSearchDto> savedSearches = Collections.emptyList();
        if (currentUserId != null) {
            savedSearches = searchRepository.findRecentSearchesByUserId(currentUserId, PageRequest.of(0, 10))
                    .stream()
                    .map(s -> SavedAndTrendingSearchesResponseDto.SavedSearchDto.builder()
                            .savedSearchId(s.getId())
                            .queryText(s.getQueryText())
                            .lastSearchedAt(s.getLastSearchedAt())
                            .build())
                    .toList();
        }

        // 2. Lấy top 10 từ khóa tìm kiếm hot nhất từ Redis Sorted Set
        List<String> trendingSearches = Collections.emptyList();
        try {
            Map<String, Double> scoresByQuery = new HashMap<>();
            LocalDate today = LocalDate.now(SEARCH_ZONE);
            for (int dayOffset = 0; dayOffset < TRENDING_WINDOW_DAYS; dayOffset++) {
                String dailyKey = TRENDING_SEARCHES_KEY_PREFIX + today.minusDays(dayOffset);
                Set<org.springframework.data.redis.core.ZSetOperations.TypedTuple<String>> dailyScores =
                        redisTemplate.opsForZSet().reverseRangeWithScores(
                                dailyKey,
                                0,
                                TRENDING_CANDIDATES_PER_DAY - 1L);
                if (dailyScores == null) {
                    continue;
                }
                for (org.springframework.data.redis.core.ZSetOperations.TypedTuple<String> tuple : dailyScores) {
                    if (tuple.getValue() != null && tuple.getScore() != null
                            && SessionCodeSearch.sessionId(SessionCodeSearch.normalize(tuple.getValue())) == null) {
                        scoresByQuery.merge(tuple.getValue(), tuple.getScore(), Double::sum);
                    }
                }
            }
            trendingSearches = scoresByQuery.entrySet().stream()
                    .sorted(Map.Entry.<String, Double>comparingByValue().reversed()
                            .thenComparing(Map.Entry.comparingByKey()))
                    .limit(10)
                    .map(Map.Entry::getKey)
                    .toList();
        } catch (Exception e) {
            log.warn("[SearchService] Không thể lấy trending searches từ Redis: {}", e.getMessage());
        }

        return SavedAndTrendingSearchesResponseDto.builder()
                .savedSearches(savedSearches)
                .trendingSearches(trendingSearches)
                .build();
    }

    @Override
    @Transactional
    public DeleteSavedSearchResponseDto deleteSavedSearch(Long id) {
        if (id == null) {
            return null;
        }
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        searchRepository.deleteByIdAndUserId(id, currentUserId);
        log.info("[SearchService] 🗑️ User ID {} đã xóa từ khóa tìm kiếm ID {}", currentUserId, id);
        return DeleteSavedSearchResponseDto.builder()
                .savedSearchId(id)
                .build();
    }

    @Override
    public void recordSearchQuery(String query) {
        if (query == null || query.isBlank())
            return;
        String cleanQuery = query.trim().replaceAll("\\s+", " ");
        if (cleanQuery.length() < 2 || cleanQuery.length() > 200
                || SessionCodeSearch.sessionId(SessionCodeSearch.normalize(cleanQuery)) != null) {
            return;
        }

        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        if (currentUserId == null) {
            return;
        }

        // 1. Mỗi người chỉ đóng góp 1 lượt/ngày cho cùng một từ khóa Trending.
        try {
            String normalizedQuery = cleanQuery.toLowerCase(Locale.ROOT);
            LocalDate today = LocalDate.now(SEARCH_ZONE);
            String seenKey = TRENDING_SEARCHES_SEEN_KEY_PREFIX
                    + today
                    + ":"
                    + currentUserId;
            Long added = redisTemplate.opsForSet().add(seenKey, normalizedQuery);
            redisTemplate.expire(seenKey, Duration.ofDays(2));
            if (added != null && added > 0) {
                String dailyTrendingKey = TRENDING_SEARCHES_KEY_PREFIX + today;
                redisTemplate.opsForZSet().incrementScore(dailyTrendingKey, normalizedQuery, 1.0);
                redisTemplate.expire(dailyTrendingKey, Duration.ofDays(TRENDING_WINDOW_DAYS + 1L));
            }
        } catch (Exception e) {
            log.warn("[SearchService] Không thể cập nhật trending search trên Redis: {}", e.getMessage());
        }

        // 2. Lưu vào lịch sử cá nhân trong PostgreSQL & Giới hạn tối đa 20 từ khóa gần
        // nhất (FIFO)
        try {
            transactionTemplate.executeWithoutResult(_ -> {
                Instant now = Instant.now();
                // 1 query UPDATE nếu từ khóa đã tồn tại
                int updated = searchRepository.updateLastSearchedAt(currentUserId, cleanQuery, now);
                if (updated == 0) {
                    // Từ khóa mới: 1 query INSERT + 1 query Native SQL Pruning (xóa ngoài top 20)
                    User userRef = userRepository.getReferenceById(currentUserId);
                    Search newSearch = Search.builder()
                            .user(userRef)
                            .queryText(cleanQuery)
                            .lastSearchedAt(now)
                            .createdAt(now)
                            .build();
                    searchRepository.save(newSearch);
                    searchRepository.pruneOldSearches(currentUserId, MAX_SAVED_SEARCHES_PER_USER);
                }
            });
        } catch (Exception e) {
            log.warn("[SearchService] Không thể lưu lịch sử tìm kiếm cho User ID {}: {}", currentUserId,
                    e.getMessage());
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private void resolveCurrentSearchData(List<GlobalSearchResponseDto.SearchResultItemDto> items) {
        var posts = items.stream()
                .map(GlobalSearchResponseDto.SearchResultItemDto::getPost)
                .filter(Objects::nonNull)
                .map(GlobalSearchResponseDto.PostSearchResultDto::getPostId)
                .toList();
        if (!posts.isEmpty()) {
            JwtPayload viewer = JwtPayload.getCurrentUserPayload();
            var countersById = postRepository.findSearchPostCounters(
                            posts, viewer.getUserId(), UserRole.ADMIN.name().equalsIgnoreCase(viewer.getRole()))
                    .stream()
                    .collect(Collectors.toMap(SearchPostCounters::getPostId, Function.identity()));
            items.removeIf(item -> {
                var post = item.getPost();
                if (post == null) return false;
                var counters = countersById.get(post.getPostId());
                if (counters == null) return true;
                post.setReactCount(counters.getReactCount());
                post.setCommentCount(counters.getCommentCount());
                return false;
            });
        }

        var questions = items.stream()
                .map(GlobalSearchResponseDto.SearchResultItemDto::getQuestion)
                .filter(Objects::nonNull)
                .map(GlobalSearchResponseDto.QuestionSearchResultDto::getQuestionId)
                .toList();
        if (!questions.isEmpty()) {
            var sessionsByQuestion = questionRepository.findSearchPracticeTargets(questions).stream()
                    .collect(Collectors.toMap(SearchPracticeTarget::getQuestionId, SearchPracticeTarget::getSessionId));
            items.removeIf(item -> {
                var question = item.getQuestion();
                if (question == null) return false;
                Long sessionId = sessionsByQuestion.get(question.getQuestionId());
                if (sessionId == null) return true;
                question.setSessionId(sessionId);
                return false;
            });
        }
    }

    private List<String> resolveTargetIndices(SearchType type) {
        if (type == null || type == SearchType.ALL) {
            return List.of(
                    UserSearchDoc.INDEX_NAME,
                    SubjectSearchDoc.INDEX_NAME,
                    PostSearchDoc.INDEX_NAME,
                    SessionSearchDoc.INDEX_NAME,
                    QuestionSearchDoc.INDEX_NAME);
        }
        return switch (type) {
            case USER -> List.of(UserSearchDoc.INDEX_NAME);
            case SUBJECT -> List.of(SubjectSearchDoc.INDEX_NAME);
            case POST -> List.of(PostSearchDoc.INDEX_NAME);
            case SESSION -> List.of(SessionSearchDoc.INDEX_NAME);
            case QUESTION -> List.of(QuestionSearchDoc.INDEX_NAME);
            default -> List.of(UserSearchDoc.INDEX_NAME, SubjectSearchDoc.INDEX_NAME, PostSearchDoc.INDEX_NAME,
                    SessionSearchDoc.INDEX_NAME, QuestionSearchDoc.INDEX_NAME);
        };
    }

    private Query buildQuery(String queryText) {
        if (queryText == null || queryText.trim().isBlank()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        String cleanText = queryText.trim();

        List<String> searchFields = List.of(
                "full_name^4", "student_lecturer_code^3", "bio^1", "school_faculty^2", "major^2",
                "subject_code^4", "subject_name^3",
                "content^4", "author_name^2",
                "title^4", "topic_name^2", "question_code^5",
                "options.text^3", "explanation^2");

        return Query.of(q -> q.multiMatch(mm -> mm
                .query(cleanText)
                .fields(searchFields)
                .fuzziness("AUTO")));
    }

    private GlobalSearchResponseDto.SearchResultItemDto mapHitToSearchResultItem(Hit<ObjectNode> hit) {
        if (hit.source() == null)
            return null;
        String index = hit.index();
        Double score = hit.score();

        try {
            if (UserSearchDoc.INDEX_NAME.equals(index)) {
                UserSearchDoc doc = jsonMapper.treeToValue(hit.source(), UserSearchDoc.class);
                return GlobalSearchResponseDto.SearchResultItemDto.builder()
                        .type(SearchType.USER)
                        .relevanceScore(score)
                        .user(GlobalSearchResponseDto.UserSearchResultDto.builder()
                                .userId(doc.getId())
                                .fullName(doc.getFullName())
                                .studentLecturerCode(doc.getStudentLecturerCode())
                                .email(doc.getEmail())
                                .bio(doc.getBio())
                                .avatarUrl(doc.getAvatarUrl())
                                .avatarFrameUrl(doc.getAvatarFrameUrl())
                                .schoolFaculty(doc.getSchoolFaculty())
                                .major(doc.getMajor())
                                .role(doc.getRole())
                                .totalProposedQuestions(doc.getTotalProposedQuestions())
                                .badgesCount(doc.getBadgesCount())
                                .followersCount(doc.getFollowersCount())
                                .build())
                        .build();

            } else if (SubjectSearchDoc.INDEX_NAME.equals(index)) {
                SubjectSearchDoc doc = jsonMapper.treeToValue(hit.source(), SubjectSearchDoc.class);
                return GlobalSearchResponseDto.SearchResultItemDto.builder()
                        .type(SearchType.SUBJECT)
                        .relevanceScore(score)
                        .subject(GlobalSearchResponseDto.SubjectSearchResultDto.builder()
                                .subjectId(doc.getId())
                                .subjectCode(doc.getSubjectCode())
                                .subjectName(doc.getSubjectName())
                                .build())
                        .build();

            } else if (PostSearchDoc.INDEX_NAME.equals(index)) {
                PostSearchDoc doc = jsonMapper.treeToValue(hit.source(), PostSearchDoc.class);
                return GlobalSearchResponseDto.SearchResultItemDto.builder()
                        .type(SearchType.POST)
                        .relevanceScore(score)
                        .post(GlobalSearchResponseDto.PostSearchResultDto.builder()
                                .postId(doc.getId())
                                .content(doc.getContent())
                                .authorId(doc.getAuthorId())
                                .authorName(doc.getAuthorName())
                                .authorAvatarUrl(doc.getAuthorAvatarUrl())
                                .authorFrameUrl(doc.getAuthorFrameUrl())
                                .postType(doc.getPostType())
                                .mediaUrls(doc.getMediaUrls())
                                .createdAt(doc.getCreatedAt())
                                .build())
                        .build();

            } else if (SessionSearchDoc.INDEX_NAME.equals(index)) {
                SessionSearchDoc doc = jsonMapper.treeToValue(hit.source(), SessionSearchDoc.class);
                return GlobalSearchResponseDto.SearchResultItemDto.builder()
                        .type(SearchType.SESSION)
                        .relevanceScore(score)
                        .session(GlobalSearchResponseDto.SessionSearchResultDto.builder()
                                .sessionId(doc.getId())
                                .title(doc.getTitle())
                                .content(doc.getContent())
                                .subjectId(doc.getSubjectId())
                                .subjectName(doc.getSubjectName())
                                .status(doc.getStatus())
                                .questionCount(doc.getQuestionCount())
                                .reactCount(doc.getReactCount())
                                .commentCount(doc.getCommentCount())
                                .createdAt(doc.getCreatedAt())
                                .build())
                        .build();

            } else if (QuestionSearchDoc.INDEX_NAME.equals(index)) {
                QuestionSearchDoc doc = jsonMapper.treeToValue(hit.source(), QuestionSearchDoc.class);

                return GlobalSearchResponseDto.SearchResultItemDto.builder()
                        .type(SearchType.QUESTION)
                        .relevanceScore(score)
                        .question(GlobalSearchResponseDto.QuestionSearchResultDto.builder()
                                .questionId(doc.getId())
                                .questionCode(doc.getQuestionCode())
                                .subjectId(doc.getSubjectId())
                                .subjectName(doc.getSubjectName())
                                .topicId(doc.getTopicId())
                                .topicName(doc.getTopicName())
                                .difficulty(doc.getDifficulty())
                                .content(doc.getContent())
                                .options(doc.getOptions() != null
                                        ? doc.getOptions().stream()
                                                .map(opt -> GlobalSearchResponseDto.QuestionOptionDto.builder()
                                                        .key(opt.getKey())
                                                        .text(opt.getText())
                                                        .mediaUrl(opt.getMediaUrl())
                                                        .build())
                                                .toList()
                                        : Collections.emptyList())
                                .imageUrls(doc.getImageUrls())
                                .avgRating(doc.getAvgRating())
                                .ratingCount(doc.getRatingCount())
                                .reactCount(doc.getReactCount())
                                .commentCount(doc.getCommentCount())
                                .createdAt(doc.getCreatedAt())
                                .build())
                        .build();
            }
        } catch (Exception e) {
            log.warn("[SearchService] Lỗi khi deserialize hit từ index {}: {}", index, e.getMessage());
        }
        return null;
    }
}
