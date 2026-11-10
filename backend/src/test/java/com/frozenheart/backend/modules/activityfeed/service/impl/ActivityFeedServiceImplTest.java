package com.frozenheart.backend.modules.activityfeed.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.session.SessionStatus;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.util.AnonymizerUtil;
import com.frozenheart.backend.modules.activityfeed.repository.ActivityFeedRepository;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.post.service.ApprovalPostAuthor;
import com.frozenheart.backend.modules.post.service.LecturerNoteAccess;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.FollowRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ActivityFeedServiceImplTest {
    @Mock ActivityFeedRepository feedRepository;
    @Mock UserProfileRepository profileRepository;
    @Mock FriendshipRepository friendshipRepository;
    @Mock FollowRepository followRepository;
    @Mock BlockRepository blockRepository;
    @Mock PostRepository postRepository;
    @Mock SessionRepository sessionRepository;
    @Mock QuestionRepository questionRepository;
    @Mock ReactRepository reactRepository;

    @AfterEach
    void clearAuthentication() {
        SecurityContextHolder.clearContext();
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void approvalEventUsesPostInteractionsAndSessionClassForNote(boolean teachesSessionClass) {
        var anonymizer = new AnonymizerUtil("unit-test");
        var service = new ActivityFeedServiceImpl(feedRepository, profileRepository, friendshipRepository,
                followRepository, blockRepository, postRepository, new LecturerNoteAccess(postRepository),
                sessionRepository, questionRepository, reactRepository, new ApprovalPostAuthor(anonymizer));
        var authentication = new UsernamePasswordAuthenticationToken("lecturer", null, List.of());
        authentication.setDetails(JwtPayload.builder().userId(7L).role("LECTURER").build());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        var session = new Session();
        session.setId(100L);
        session.setStatus(SessionStatus.RESOLVED);
        var student = User.builder().id(5L).email("student@example.test").build();
        session.setProposer(student);
        session.setReviewer(User.builder().id(9L).build());
        var post = Post.builder().id(200L).session(session).postType(PostType.QUESTION_APPROVED_NOTIFICATION)
                .visibility(PostVisibility.PUBLIC).reactCount(5).commentCount(2).build();
        var event = ActivityFeed.builder().id(300L).targetType(ActivityFeedTargetType.SESSION)
                .actionType(ActionType.SESSION_RESOLVED_APPROVED).targetId(100L).user(student).build();
        var reaction = new React();
        reaction.setTargetId(200L);
        reaction.setReactionType(ReactionType.LOVE);
        when(feedRepository.findPersonalizedFeeds(anyLong(), any(), any(), anyBoolean(), any())).thenReturn(List.of(event));
        when(postRepository.findPublicApprovalPostsBySessionIds(List.of(100L))).thenReturn(List.of(post));
        when(postRepository.findApprovalPostIdsForLecturer(List.of(200L), 7L))
                .thenReturn(teachesSessionClass ? List.of(200L) : List.of());
        when(reactRepository.findUserReactsByTargetIds(7L, InteractionTargetType.POST, List.of(200L)))
                .thenReturn(List.of(reaction));

        var item = service.getActivityFeeds(null, 20).items().getFirst();
        var content = (Map<?, ?>) item.getContent();

        assertEquals(300L, item.getFeedId());
        assertEquals(ActivityFeedTargetType.POST, item.getTargetType());
        assertEquals(200L, item.getTargetId());
        assertEquals(anonymizer.encodeUserId(5L), item.getActor().getFullName());
        assertNull(item.getActor().getUserId());
        assertNull(item.getActor().getAvatarUrl());
        assertNull(item.getActor().getFrameUrl());
        assertEquals(100L, content.get("session_id"));
        assertEquals(5, content.get("react_count"));
        assertEquals(2, content.get("comment_count"));
        assertEquals("LOVE", content.get("my_reaction"));
        assertEquals(teachesSessionClass, content.get("can_edit_lecturer_note"));
        verifyNoInteractions(sessionRepository, profileRepository);
    }
}
