package com.frozenheart.backend.modules.activityfeed.service.impl;

import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeed;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedMetaData;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.core.entity.session.Question;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.session.Session;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.React;
import com.frozenheart.backend.core.entity.socialinteraction.ReactionType;
import com.frozenheart.backend.modules.activityfeed.dto.*;
import com.frozenheart.backend.modules.activityfeed.repository.ActivityFeedRepository;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.friendship.repository.FriendshipRepository;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.session.repository.SessionRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.ReactRepository;
import com.frozenheart.backend.modules.user.repository.BlockRepository;
import com.frozenheart.backend.modules.user.repository.FollowRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ActivityFeedServiceImpl implements ActivityFeedService {

    private final ActivityFeedRepository activityFeedRepository;
    private final UserProfileRepository userProfileRepository;
    private final FriendshipRepository friendshipRepository;
    private final FollowRepository followRepository;
    private final BlockRepository blockRepository;
    private final PostRepository postRepository;
    private final SessionRepository sessionRepository;
    private final QuestionRepository questionRepository;
    private final ReactRepository reactRepository;

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<ActivityFeedItemDto> getActivityFeeds(Long after, Integer limit) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;

        Optional<JwtPayload> payloadOpt = getOptionalJwtPayload();
        Long currentUserId = payloadOpt.map(JwtPayload::getUserId).orElse(null);

        List<ActivityFeed> feeds;
        if (currentUserId != null) {
            Set<Long> blockedIds = blockRepository.findAllBlockedUserIds(currentUserId);
            Set<Long> friendIds = friendshipRepository.findAllAcceptedFriendUserIds(currentUserId);
            Set<Long> followingIds = followRepository.findAllFollowedUserIds(currentUserId);

            Set<Long> networkIds = new HashSet<>();
            networkIds.add(currentUserId);
            if (friendIds != null)
                networkIds.addAll(friendIds);
            if (followingIds != null)
                networkIds.addAll(followingIds);
            if (blockedIds != null && !blockedIds.isEmpty()) {
                networkIds.removeAll(blockedIds);
            }

            boolean hasBlocked = (blockedIds != null && !blockedIds.isEmpty());
            Collection<Long> safeBlockedIds = hasBlocked ? blockedIds : Collections.singleton(0L);

            feeds = activityFeedRepository.findPersonalizedFeeds(cursor, networkIds, safeBlockedIds, hasBlocked,
                    pageable);
        } else {
            feeds = activityFeedRepository.findByIdLessThanOrderByIdDesc(cursor, pageable);
        }

        boolean hasNext = false;
        Long nextCursor = null;

        if (feeds.size() > pageSize) {
            hasNext = true;
            feeds = feeds.subList(0, pageSize);
            nextCursor = feeds.getLast().getId();
        }

        List<Long> actorIds = feeds.stream()
                .map(ActivityFeed::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .toList();

        Map<Long, UserProfile> profileMap = actorIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(actorIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // Batch Enrichment: Gom các targetIds theo từng loại để query 1 lần (Tuyệt đối chống N+1)
        List<Long> postIds = feeds.stream()
                .filter(f -> f.getTargetType() == ActivityFeedTargetType.POST && f.getTargetId() != null)
                .map(ActivityFeed::getTargetId)
                .distinct()
                .toList();

        List<Long> questionIds = feeds.stream()
                .filter(f -> f.getTargetType() == ActivityFeedTargetType.QUESTION && f.getTargetId() != null)
                .map(ActivityFeed::getTargetId)
                .distinct()
                .toList();

        List<Long> sessionIds = feeds.stream()
                .filter(f -> f.getTargetType() == ActivityFeedTargetType.SESSION && f.getTargetId() != null)
                .map(ActivityFeed::getTargetId)
                .distinct()
                .toList();

        Map<Long, Post> postMap = postIds.isEmpty() ? Map.of()
                : postRepository.findByIdInAndDeletedAtIsNull(postIds).stream()
                        .collect(Collectors.toMap(Post::getId, p -> p));

        Map<Long, Session> sessionMap = sessionIds.isEmpty() ? Map.of()
                : sessionRepository.findAllById(sessionIds).stream()
                        .collect(Collectors.toMap(Session::getId, s -> s));

        Map<Long, Question> questionMap = questionIds.isEmpty() ? Map.of()
                : questionRepository.findAllById(questionIds).stream()
                        .collect(Collectors.toMap(Question::getId, q -> q));

        Map<Long, ReactionType> userPostReactMap = (currentUserId != null && !postIds.isEmpty())
                ? reactRepository.findUserReactsByTargetIds(currentUserId, InteractionTargetType.POST, postIds).stream()
                        .collect(Collectors.toMap(React::getTargetId, React::getReactionType, (r1, _) -> r1))
                : Map.of();

        Map<Long, ReactionType> userSessionReactMap = (currentUserId != null && !sessionIds.isEmpty())
                ? reactRepository.findUserReactsByTargetIds(currentUserId, InteractionTargetType.SESSION, sessionIds).stream()
                        .collect(Collectors.toMap(React::getTargetId, React::getReactionType, (r1, _) -> r1))
                : Map.of();

        Map<Long, ReactionType> userQuestionReactMap = (currentUserId != null && !questionIds.isEmpty())
                ? reactRepository.findUserReactsByTargetIds(currentUserId, InteractionTargetType.QUESTION, questionIds).stream()
                .collect(Collectors.toMap(React::getTargetId, React::getReactionType, (r1, _) -> r1))
                : Map.of();

        List<ActivityFeedItemDto> items = feeds.stream()
                .map(feed -> mapToItemDto(feed, profileMap, postMap, sessionMap, questionMap,
                        userPostReactMap, userSessionReactMap, userQuestionReactMap))
                .toList();

        return CursorResponse.<ActivityFeedItemDto>builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public NewFeedCountResponseDto getNewFeedCount(Long since) {
        int count = 0;
        if (since != null && since > 0) {
            Instant sinceTime = Instant.ofEpochMilli(since);
            Optional<JwtPayload> payloadOpt = getOptionalJwtPayload();
            Long currentUserId = payloadOpt.map(JwtPayload::getUserId).orElse(null);

            if (currentUserId != null) {
                Set<Long> blockedIds = blockRepository.findAllBlockedUserIds(currentUserId);
                Set<Long> friendIds = friendshipRepository.findAllAcceptedFriendUserIds(currentUserId);
                Set<Long> followingIds = followRepository.findAllFollowedUserIds(currentUserId);

                Set<Long> networkIds = new HashSet<>();
                networkIds.add(currentUserId);
                if (friendIds != null)
                    networkIds.addAll(friendIds);
                if (followingIds != null)
                    networkIds.addAll(followingIds);
                if (blockedIds != null && !blockedIds.isEmpty()) {
                    networkIds.removeAll(blockedIds);
                }

                boolean hasBlocked = (blockedIds != null && !blockedIds.isEmpty());
                Collection<Long> safeBlockedIds = hasBlocked ? blockedIds : Collections.singleton(0L);

                count = activityFeedRepository.countPersonalizedNewFeeds(sinceTime, networkIds, safeBlockedIds,
                        hasBlocked);
            } else {
                count = activityFeedRepository.countByCreatedAtAfter(sinceTime);
            }
        }
        return NewFeedCountResponseDto.builder()
                .newCount(count)
                .build();
    }

    private Optional<JwtPayload> getOptionalJwtPayload() {
        try {
            return Optional.ofNullable(JwtPayload.getCurrentUserPayload());
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    @Override
    @Transactional
    public void logActivity(User actor, ActionType actionType, ActivityFeedTargetType targetType, Long targetId,
            ActivityFeedMetaData metaData) {
        ActivityFeed feed = ActivityFeed.builder()
                .user(actor)
                .actionType(actionType)
                .targetType(targetType)
                .targetId(targetId)
                .metaData(metaData)
                .createdAt(Instant.now())
                .build();

        activityFeedRepository.save(feed);
    }

    private ActivityFeedItemDto mapToItemDto(
            ActivityFeed feed,
            Map<Long, UserProfile> profileMap,
            Map<Long, Post> postMap,
            Map<Long, Session> sessionMap,
            Map<Long, Question> questionMap,
            Map<Long, ReactionType> userPostReactMap,
            Map<Long, ReactionType> userSessionReactMap,
            Map<Long, ReactionType> userQuestionReactMap) {

        AuthorDto actorDto;
        if (feed.getUser() != null) {
            UserProfile profile = profileMap.get(feed.getUser().getId());
            actorDto = AuthorDto.builder()
                    .userId(feed.getUser().getId())
                    .fullName(profile != null ? profile.getFullName() : feed.getUser().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .build();
        } else {
            actorDto = AuthorDto.getSystemAuthor();
        }

        Map<String, Object> contentMap = new LinkedHashMap<>();
        if (feed.getMetaData() != null) {
            ActivityFeedMetaData m = feed.getMetaData();
            if (m.getTitle() != null)
                contentMap.put("title", m.getTitle());
            if (m.getDescription() != null)
                contentMap.put("description", m.getDescription());
            if (m.getMediaUrl() != null)
                contentMap.put("media_url", m.getMediaUrl());
            if (m.getBadgeName() != null)
                contentMap.put("badge_name", m.getBadgeName());
            if (m.getCosmeticName() != null)
                contentMap.put("cosmetic_name", m.getCosmeticName());
            if (m.getSubjectCode() != null)
                contentMap.put("subject_code", m.getSubjectCode());
            if (m.getSubjectName() != null)
                contentMap.put("subject_name", m.getSubjectName());
        }

        // Tự động enrich chỉ số tương tác theo targetType
        ActivityFeedTargetType targetType = feed.getTargetType();
        Long targetId = feed.getTargetId();

        if (targetType == ActivityFeedTargetType.POST && targetId != null) {
            Post post = postMap.get(targetId);
            if (post != null) {
                contentMap.put("comment_count", post.getCommentCount());
                contentMap.put("react_count", post.getReactCount());
                if (post.getVisibility() != null) {
                    contentMap.put("visibility", post.getVisibility().name());
                }
                if (post.getMediaUrls() != null && !post.getMediaUrls().isEmpty()) {
                    contentMap.put("media_urls", post.getMediaUrls());
                }
                if (post.getLecturerNote() != null) {
                    contentMap.put("lecturer_note", post.getLecturerNote());
                    if (post.getNotedLecturer() != null) {
                        UserProfile lProfile = profileMap.get(post.getNotedLecturer().getId());
                        contentMap.put("noted_lecturer", AuthorDto.builder()
                                .userId(post.getNotedLecturer().getId())
                                .fullName(lProfile != null ? lProfile.getFullName() : post.getNotedLecturer().getEmail())
                                .avatarUrl(lProfile != null ? lProfile.getAvatarUrl() : null)
                                .frameUrl(lProfile != null ? lProfile.getAvatarFrameUrl() : null)
                                .build());
                    }
                }
            } else {
                contentMap.put("comment_count", 0);
                contentMap.put("react_count", 0);
            }
            ReactionType userReact = userPostReactMap.get(targetId);
            contentMap.put("my_reaction", userReact != null ? userReact.name() : null);
            contentMap.put("reacted_by_me", userReact != null);
        } else if (targetType == ActivityFeedTargetType.SESSION && targetId != null) {
            Session session = sessionMap.get(targetId);
            contentMap.put("comment_count", session != null ? session.getCommentCount() : 0);
            contentMap.put("react_count", session != null ? session.getReactCount() : 0);
            ReactionType userReact = userSessionReactMap.get(targetId);
            contentMap.put("my_reaction", userReact != null ? userReact.name() : null);
            contentMap.put("reacted_by_me", userReact != null);
        } else if (targetType == ActivityFeedTargetType.QUESTION && targetId != null) {
            Question question = questionMap.get(targetId);
            contentMap.put("comment_count", question != null ? question.getCommentCount() : 0);
            contentMap.put("react_count", question != null ? question.getReactCount() : 0);
            ReactionType userReact = userQuestionReactMap.get(targetId);
            contentMap.put("my_reaction", userReact != null ? userReact.name() : null);
            contentMap.put("reacted_by_me", userReact != null);
        }

        return ActivityFeedItemDto.builder()
                .feedId(feed.getId())
                .targetType(feed.getTargetType())
                .targetId(feed.getTargetId())
                .actionType(feed.getActionType())
                .actor(actorDto)
                .content(contentMap)
                .createdAt(feed.getCreatedAt())
                .weight(1)
                .build();
    }
}
