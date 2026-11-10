package com.frozenheart.backend.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.util.ObjectBuilder;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.Search;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository.SearchPostCounters;
import com.frozenheart.backend.modules.search.document.PostSearchDoc;
import com.frozenheart.backend.modules.search.document.QuestionSearchDoc;
import com.frozenheart.backend.modules.search.dto.GlobalSearchResponseDto;
import com.frozenheart.backend.modules.search.dto.SearchType;
import com.frozenheart.backend.modules.search.repository.SearchRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository.SearchPracticeTarget;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.session.dto.SubmissionProjection;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceImplTest {

    @Mock
    private ElasticsearchClient client;
    @Mock
    private JsonMapper jsonMapper;
    @Mock
    private SearchRepository searchRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RedisTemplate<String, String> redisTemplate;
    @Mock
    private SetOperations<String, String> setOperations;
    @Mock
    private ZSetOperations<String, String> zSetOperations;
    @Mock
    private TransactionTemplate transactionTemplate;
    @Mock
    private PostRepository postRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private SessionRepository sessionRepository;

    @InjectMocks
    private SearchServiceImpl searchService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Live search không được ghi lịch sử hoặc tăng Trending")
    void liveSearchShouldRemainReadOnly() {
        GlobalSearchResponseDto response = searchService.search("nguyễn tiến", SearchType.ALL, null, 6);

        assertNotNull(response);
        assertEquals(0, response.getItems().size());
        verifyNoInteractions(searchRepository, userRepository, redisTemplate, transactionTemplate);
    }

    @Test
    void exactCodeSearchReturnsPrivateSessionOnlyWhenRepositoryAuthorizesViewer() {
        String code = "IT4409_4E7Xer_1794274070936_452";
        SubmissionProjection row = mock(SubmissionProjection.class);
        when(row.getSessionId()).thenReturn(452L);
        when(row.getSessionCode()).thenReturn(code);
        when(row.getStatus()).thenReturn(SessionStatus.REVIEWING);
        when(sessionRepository.findVisibleSessionByCode(452L, code, 7L)).thenReturn(Optional.of(row));
        when(sessionRepository.findVisibleSessionByCode(452L, code, 8L)).thenReturn(Optional.empty());

        setCurrentUser(7L);
        var ownerResult = searchService.search(" #" + code + " ", SearchType.ALL, null, 10);
        assertEquals(code, ownerResult.getItems().getFirst().getSession().getSessionCode());
        assertEquals(SessionStatus.REVIEWING, ownerResult.getItems().getFirst().getSession().getStatus());

        setCurrentUser(8L);
        assertEquals(List.of(), searchService.search(code, SearchType.SESSION, null, 10).getItems());
        verifyNoInteractions(client);
    }

    @Test
    void confirmedSessionCodeSearchDoesNotPublishTheCodeAsTrending() {
        setCurrentUser(7L);
        searchService.recordSearchQuery("#IT4409_4E7Xer_1794274070936_452");
        verifyNoInteractions(redisTemplate, searchRepository, transactionTemplate);
    }

    @Test
    @DisplayName("Tìm kiếm lấy bộ đếm bài viết và đích luyện tập theo lô từ PostgreSQL")
    void searchUsesCurrentPostCountersAndPublicPracticeTargets() throws Exception {
        setCurrentUser(7L);
        ObjectNode postSource = mock(ObjectNode.class);
        ObjectNode hiddenPostSource = mock(ObjectNode.class);
        ObjectNode questionSource = mock(ObjectNode.class);
        var hits = List.of(
                Hit.<ObjectNode>of(h -> h.index(PostSearchDoc.INDEX_NAME).id("10").source(postSource)),
                Hit.<ObjectNode>of(h -> h.index(PostSearchDoc.INDEX_NAME).id("11").source(hiddenPostSource)),
                Hit.<ObjectNode>of(h -> h.index(QuestionSearchDoc.INDEX_NAME).id("20").source(questionSource)));
        var response = SearchResponse.<ObjectNode>of(r -> r
                .took(1).timedOut(false)
                .shards(s -> s.total(1).successful(1).failed(0))
                .hits(h -> h.hits(hits)));
        when(client.search(org.mockito.ArgumentMatchers
                .<Function<SearchRequest.Builder, ObjectBuilder<SearchRequest>>>any(),
                eq(ObjectNode.class))).thenReturn(response);
        when(jsonMapper.treeToValue(postSource, PostSearchDoc.class))
                .thenReturn(PostSearchDoc.builder().id(10L).reactCount(0).commentCount(0).build());
        when(jsonMapper.treeToValue(hiddenPostSource, PostSearchDoc.class))
                .thenReturn(PostSearchDoc.builder().id(11L).build());
        when(jsonMapper.treeToValue(questionSource, QuestionSearchDoc.class))
                .thenReturn(QuestionSearchDoc.builder().id(20L).build());
        SearchPostCounters counters = mock(SearchPostCounters.class);
        when(counters.getPostId()).thenReturn(10L);
        when(counters.getReactCount()).thenReturn(4);
        when(counters.getCommentCount()).thenReturn(3);
        when(postRepository.findSearchPostCounters(List.of(10L, 11L), 7L, false)).thenReturn(List.of(counters));
        SearchPracticeTarget target = mock(SearchPracticeTarget.class);
        when(target.getQuestionId()).thenReturn(20L);
        when(target.getSessionId()).thenReturn(30L);
        when(questionRepository.findSearchPracticeTargets(List.of(20L))).thenReturn(List.of(target));

        var items = searchService.search("từ khóa", SearchType.ALL, null, 6).getItems();

        assertEquals(2, items.size());
        assertEquals(4, items.getFirst().getPost().getReactCount());
        assertEquals(3, items.getFirst().getPost().getCommentCount());
        assertEquals(30L, items.getLast().getQuestion().getSessionId());
    }

    @Test
    @DisplayName("Tìm kiếm đã xác nhận được chuẩn hóa, lưu lịch sử và tăng Trending v2")
    void confirmedSearchShouldRecordNormalizedQuery() {
        long userId = 7L;
        setCurrentUser(userId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForZSet()).thenReturn(zSetOperations);
        when(setOperations.add(anyString(), eq("nguyễn tiến thành"))).thenReturn(1L);
        when(searchRepository.updateLastSearchedAt(eq(userId), eq("Nguyễn Tiến Thành"), any()))
                .thenReturn(0);
        when(userRepository.getReferenceById(userId))
                .thenReturn(User.builder().id(userId).email("student@sqb.edu.vn").build());
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(org.mockito.Mockito.mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());

        searchService.recordSearchQuery("  Nguyễn   Tiến Thành  ");

        String dailyTrendingKey = "trending_searches:v2:"
                + LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        verify(zSetOperations).incrementScore(dailyTrendingKey, "nguyễn tiến thành", 1.0);
        verify(redisTemplate).expire(anyString(), eq(Duration.ofDays(2)));
        verify(redisTemplate).expire(dailyTrendingKey, Duration.ofDays(8));

        ArgumentCaptor<Search> savedSearch = ArgumentCaptor.forClass(Search.class);
        verify(searchRepository).save(savedSearch.capture());
        assertEquals("Nguyễn Tiến Thành", savedSearch.getValue().getQueryText());
        assertEquals(userId, savedSearch.getValue().getUser().getId());
    }

    @Test
    @DisplayName("Một người tìm lại cùng từ khóa trong ngày không được tăng Trending lần hai")
    void repeatedConfirmedSearchShouldNotInflateTrending() {
        long userId = 7L;
        setCurrentUser(userId);

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.add(anyString(), eq("java"))).thenReturn(0L);

        searchService.recordSearchQuery("Java");

        verify(redisTemplate, never()).opsForZSet();
        verifyNoInteractions(zSetOperations);
    }

    private void setCurrentUser(Long userId) {
        JwtPayload payload = JwtPayload.builder()
                .userId(userId)
                .role(UserRole.STUDENT.name())
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("student", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
