package com.frozenheart.backend.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.entity.session.QuestionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.Search;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.modules.search.document.*;
import com.frozenheart.backend.modules.search.dto.*;
import com.frozenheart.backend.modules.search.repository.SearchRepository;
import com.frozenheart.backend.modules.search.service.SearchService;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final ElasticsearchClient client;
    private final JsonMapper jsonMapper;
    private final SearchRepository searchRepository;
    private final UserRepository userRepository;
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TRENDING_SEARCHES_KEY = "trending_searches";
    private static final int MAX_SAVED_SEARCHES_PER_USER = 20;

    @Override
    public GlobalSearchResponseDto search(
            String query,
            SearchType type,
            SearchScope scope,
            Long after,
            Integer limit
    ) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        int fromOffset = (after != null && after >= 0) ? after.intValue() : 0;

        // Trích xuất thông tin người dùng an toàn từ Security Context
        Optional<JwtPayload> payloadOpt = getOptionalJwtPayload();
        Long currentUserId = payloadOpt.map(JwtPayload::getUserId).orElse(null);
        String currentUserRole = payloadOpt.map(JwtPayload::getRole).orElse("STUDENT");

        // Phân quyền ngữ cảnh tìm kiếm câu hỏi
        boolean isLecturerOrAdmin = "LECTURER".equalsIgnoreCase(currentUserRole)
                || "ADMIN".equalsIgnoreCase(currentUserRole);

        SearchScope effectiveScope = (scope == SearchScope.CORE && isLecturerOrAdmin)
                ? SearchScope.CORE
                : SearchScope.PUBLIC;

        // Lưu vết lịch sử và tăng điểm thịnh hành
        if (query != null && !query.trim().isBlank()) {
            recordSearchQuery(currentUserId, query.trim());
        }

        // Xác định danh sách Index cần truy vấn
        List<String> indices = resolveTargetIndices(type);

        try {
            Query esQuery = buildQuery(query, effectiveScope);

            SearchResponse<ObjectNode> response = client.search(s -> s
                    .index(indices)
                    .query(esQuery)
                    .from(fromOffset)
                    .size(pageSize + 1)
                    .sort(so -> so.score(sc -> sc.order(SortOrder.Desc))),
                    ObjectNode.class
            );

            List<Hit<ObjectNode>> hits = response.hits().hits();
            boolean hasNext = hits.size() > pageSize;
            List<Hit<ObjectNode>> resultHits = hasNext ? hits.subList(0, pageSize) : hits;

            List<GlobalSearchResponseDto.SearchResultItemDto> items = new ArrayList<>();
            for (Hit<ObjectNode> hit : resultHits) {
                GlobalSearchResponseDto.SearchResultItemDto item = mapHitToSearchResultItem(hit, effectiveScope);
                if (item != null) {
                    items.add(item);
                }
            }

            long totalHits = response.hits().total() != null ? response.hits().total().value() : items.size();
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
                            .id(s.getId())
                            .queryText(s.getQueryText())
                            .lastSearchedAt(s.getLastSearchedAt())
                            .build())
                    .toList();
        }

        // 2. Lấy top 10 từ khóa tìm kiếm hot nhất từ Redis Sorted Set
        List<String> trendingSearches = Collections.emptyList();
        try {
            Set<String> topTrending = redisTemplate.opsForZSet().reverseRange(TRENDING_SEARCHES_KEY, 0, 9);
            if (topTrending != null && !topTrending.isEmpty()) {
                trendingSearches = new ArrayList<>(topTrending);
            }
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
    public void deleteSavedSearch(Long id) {
        if (id == null) return;
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        searchRepository.deleteByIdAndUserId(id, currentUserId);
        log.info("[SearchService] 🗑️ User ID {} đã xóa từ khóa tìm kiếm ID {}", currentUserId, id);
    }

    @Override
    @Transactional
    public void recordSearchQuery(Long currentUserId, String query) {
        if (query == null || query.isBlank()) return;
        String cleanQuery = query.trim();

        // 1. Tăng điểm Trending trên Redis (ZINCRBY)
        try {
            redisTemplate.opsForZSet().incrementScore(TRENDING_SEARCHES_KEY, cleanQuery.toLowerCase(), 1.0);
        } catch (Exception e) {
            log.warn("[SearchService] Không thể cập nhật trending search trên Redis: {}", e.getMessage());
        }

        // 2. Lưu vào lịch sử cá nhân trong PostgreSQL & Giới hạn tối đa 20 từ khóa gần nhất (FIFO)
        if (currentUserId != null) {
            try {
                LocalDateTime now = LocalDateTime.now();
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
            } catch (Exception e) {
                log.warn("[SearchService] Không thể lưu lịch sử tìm kiếm cho User ID {}: {}", currentUserId, e.getMessage());
            }
        }
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private Optional<JwtPayload> getOptionalJwtPayload() {
        try {
            return Optional.ofNullable(JwtPayload.getCurrentUserPayload());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    private List<String> resolveTargetIndices(SearchType type) {
        if (type == null || type == SearchType.ALL) {
            return List.of(
                    UserSearchDoc.INDEX_NAME,
                    SubjectSearchDoc.INDEX_NAME,
                    PostSearchDoc.INDEX_NAME,
                    SessionSearchDoc.INDEX_NAME,
                    QuestionSearchDoc.INDEX_NAME
            );
        }
        return switch (type) {
            case USER -> List.of(UserSearchDoc.INDEX_NAME);
            case SUBJECT -> List.of(SubjectSearchDoc.INDEX_NAME);
            case POST -> List.of(PostSearchDoc.INDEX_NAME);
            case SESSION -> List.of(SessionSearchDoc.INDEX_NAME);
            case QUESTION -> List.of(QuestionSearchDoc.INDEX_NAME);
            default -> List.of(UserSearchDoc.INDEX_NAME, SubjectSearchDoc.INDEX_NAME, PostSearchDoc.INDEX_NAME, SessionSearchDoc.INDEX_NAME, QuestionSearchDoc.INDEX_NAME);
        };
    }

    private Query buildQuery(String queryText, SearchScope scope) {
        if (queryText == null || queryText.trim().isBlank()) {
            return Query.of(q -> q.matchAll(m -> m));
        }

        String cleanText = queryText.trim();

        List<String> searchFields = new ArrayList<>(List.of(
                "full_name^4", "student_lecturer_code^3", "bio^1", "faculty^2", "major^2",
                "subject_code^4", "subject_name^3",
                "content^3", "author_name^2",
                "title^4", "topic_name^2", "question_code^5"
        ));

        if (scope == SearchScope.CORE) {
            searchFields.add("core_content^4");
            searchFields.add("core_options_text^3");
            searchFields.add("core_explanation^2");
            searchFields.add("original_content^2");
        } else {
            searchFields.add("original_content^4");
            searchFields.add("original_options_text^3");
            searchFields.add("original_explanation^2");
        }

        return Query.of(q -> q.bool(b -> {
            b.should(s -> s.multiMatch(mm -> mm
                    .query(cleanText)
                    .fields(searchFields)
                    .fuzziness("AUTO")
            ));

            // Nếu tìm kiếm chuyên sâu trong CORE đề thi -> chỉ hiển thị câu hỏi APPROVED
            if (scope == SearchScope.CORE) {
                b.filter(f -> f.bool(fb -> fb
                        .should(sh -> sh.term(t -> t.field("status").value(QuestionStatus.APPROVED.name())))
                        .should(sh -> sh.bool(notQ -> notQ.mustNot(mn -> mn.exists(ex -> ex.field("status")))))
                ));
            }

            return b;
        }));
    }

    private GlobalSearchResponseDto.SearchResultItemDto mapHitToSearchResultItem(Hit<ObjectNode> hit, SearchScope scope) {
        if (hit.source() == null) return null;
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
                                .faculty(doc.getFaculty())
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
                                .reactCount(doc.getReactCount())
                                .commentCount(doc.getCommentCount())
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

                String resolvedContent = (scope == SearchScope.CORE && doc.getCoreContent() != null)
                        ? doc.getCoreContent()
                        : doc.getOriginalContent();

                List<String> resolvedOptions = (scope == SearchScope.CORE && doc.getCoreOptionsText() != null && !doc.getCoreOptionsText().isEmpty())
                        ? doc.getCoreOptionsText()
                        : doc.getOriginalOptionsText();

                String resolvedExplanation = (scope == SearchScope.CORE && doc.getCoreExplanation() != null)
                        ? doc.getCoreExplanation()
                        : doc.getOriginalExplanation();

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
                                .status(doc.getStatus())
                                .difficulty(doc.getDifficulty())
                                .content(resolvedContent)
                                .optionsText(resolvedOptions)
                                .explanation(resolvedExplanation)
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
