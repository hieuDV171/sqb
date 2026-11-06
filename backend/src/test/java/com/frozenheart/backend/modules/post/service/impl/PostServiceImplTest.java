package com.frozenheart.backend.modules.post.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.util.MetricService;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.post.dto.PostResponseDto;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PostServiceImplTest {

    @Mock PostRepository postRepository;
    @Mock UserRepository userRepository;
    @Mock UserProfileRepository userProfileRepository;
    @Mock SubjectRepository subjectRepository;
    @Mock MediaService mediaService;
    @Mock ActivityFeedService activityFeedService;
    @Mock ApplicationEventPublisher eventPublisher;
    @Mock BlockRepository blockRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock ReactRepository reactRepository;
    @Mock MetricService metricService;

    @InjectMocks PostServiceImpl service;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest(name = "GET detail preserves reaction {0}")
    @MethodSource("reactionCases")
    void getPostByIdReturnsExactReactionAfterReload(ReactionType reactionType) {
        long userId = 7L;
        long postId = 42L;
        setCurrentUser(userId);
        Post post = post(postId, userId);

        when(postRepository.findByIdAndDeletedAtIsNull(postId)).thenReturn(Optional.of(post));
        when(reactRepository.findByUserIdAndTargetTypeAndTargetId(
                userId, InteractionTargetType.POST, postId))
                .thenReturn(reactionType == null
                        ? Optional.empty()
                        : Optional.of(react(postId, reactionType)));
        when(userProfileRepository.findAllById(any())).thenReturn(List.of(profile(userId)));

        PostResponseDto response = service.getPostById(postId);

        assertEquals(reactionType, response.getMyReaction());
        assertEquals(reactionType != null, response.isReactedByMe());
    }

    @Test
    void getUserPostsRejectsBlockedViewerBeforeReadingPosts() {
        setCurrentUser(7L);
        when(blockRepository.isBlockedBetween(7L, 8L)).thenReturn(true);

        AppException error = assertThrows(AppException.class, () -> service.getUserPosts(8L, null, 10));

        assertEquals(ResponseCode.USER_IS_BLOCKED, error.getResponseCode());
        verifyNoInteractions(postRepository, friendshipRepository, reactRepository);
    }

    @Test
    void getMyPostsLoadsReactionTypesAndProfilesInBatches() {
        long userId = 7L;
        Post firstPost = post(42L, userId);
        Post secondPost = post(41L, userId);
        setCurrentUser(userId);

        when(postRepository.findByPosterIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                eq(userId), eq(Long.MAX_VALUE), any(Pageable.class)))
                .thenReturn(List.of(firstPost, secondPost));
        when(reactRepository.findUserReactsByTargetIds(
                userId, InteractionTargetType.POST, List.of(42L, 41L)))
                .thenReturn(List.of(react(42L, ReactionType.LOVE)));
        when(userProfileRepository.findAllById(any())).thenReturn(List.of(profile(userId)));

        CursorResponse<PostResponseDto> response = service.getMyPosts(null, 10);

        assertEquals(2, response.items().size());
        assertEquals(ReactionType.LOVE, response.items().get(0).getMyReaction());
        assertTrue(response.items().get(0).isReactedByMe());
        assertNull(response.items().get(1).getMyReaction());
        assertFalse(response.items().get(1).isReactedByMe());
        verify(reactRepository).findUserReactsByTargetIds(
                userId, InteractionTargetType.POST, List.of(42L, 41L));
        verify(userProfileRepository).findAllById(any());
    }

    private static Stream<Arguments> reactionCases() {
        return Stream.of(
                Arguments.of((ReactionType) null),
                Arguments.of(ReactionType.LIKE),
                Arguments.of(ReactionType.LOVE),
                Arguments.of(ReactionType.WOW));
    }

    private void setCurrentUser(long userId) {
        JwtPayload payload = JwtPayload.builder()
                .userId(userId)
                .role(UserRole.STUDENT.name())
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("student", null, List.of());
        authentication.setDetails(payload);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    private Post post(long postId, long authorId) {
        User author = User.builder()
                .id(authorId)
                .email("student@example.com")
                .role(UserRole.STUDENT)
                .build();
        return Post.builder()
                .id(postId)
                .poster(author)
                .postType(PostType.SOCIAL_POST)
                .visibility(PostVisibility.PUBLIC)
                .reactCount(5)
                .commentCount(2)
                .build();
    }

    private React react(long postId, ReactionType reactionType) {
        React react = new React();
        react.setTargetId(postId);
        react.setTargetType(InteractionTargetType.POST);
        react.setReactionType(reactionType);
        return react;
    }

    private UserProfile profile(long userId) {
        return UserProfile.builder()
                .userId(userId)
                .fullName("Student")
                .build();
    }
}
