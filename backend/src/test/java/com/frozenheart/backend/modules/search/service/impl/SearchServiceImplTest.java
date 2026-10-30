package com.frozenheart.backend.modules.search.service.impl;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.socialinteraction.Search;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.modules.search.dto.GlobalSearchResponseDto;
import com.frozenheart.backend.modules.search.dto.SearchType;
import com.frozenheart.backend.modules.search.repository.SearchRepository;
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

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
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
