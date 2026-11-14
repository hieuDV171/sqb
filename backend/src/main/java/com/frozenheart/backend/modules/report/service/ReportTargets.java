package com.frozenheart.backend.modules.report.service;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.socialinteraction.ModerationAction;
import com.frozenheart.backend.core.entity.socialinteraction.ReportSnapshot;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.report.constant.ReportTargetType;
import com.frozenheart.backend.modules.report.dto.AdminReportDto.TargetState;
import com.frozenheart.backend.modules.session.repository.QuestionRepository;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.socialinteraction.service.helper.InteractionTargetValidator;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ReportTargets {
    private static final int SNAPSHOT_LIMIT = 4_000;
    private final PostRepository posts;
    private final CommentRepository comments;
    private final UserRepository users;
    private final UserProfileRepository profiles;
    private final InteractionTargetValidator discussions;
    private final QuestionRepository questions;

    public record Key(String type, Long id) {}

    public record View(ReportSnapshot snapshot, TargetState state, Long actionId, Instant suspendedUntil,
                       UserRole role) {
        public static View unavailable() {
            return new View(null, TargetState.UNAVAILABLE, null, null, null);
        }
    }

    public ReportSnapshot snapshotForSubmission(ReportTargetType type, Long id, JwtPayload viewer) {
        return switch (type) {
            case POST -> postSnapshot(visiblePost(id, viewer));
            case COMMENT -> {
                Comment comment = comments.findByIdAndDeletedAtIsNull(id)
                        .filter(item -> item.getActiveModerationAction() == null).orElseThrow(ReportTargets::missing);
                validateDiscussion(comment, viewer);
                yield commentSnapshot(comment);
            }
            case USER -> {
                if (id.equals(viewer.getUserId())) throw new AppException(ResponseCode.CANNOT_INTERACT_WITH_SELF);
                UserProfile profile = profiles.findByUserIdWithUser(id)
                        .filter(item -> item.getUser().isActive()).orElseThrow(ReportTargets::missing);
                yield userSnapshot(profile.getUser(), profile);
            }
        };
    }

    public Map<Key, View> load(Collection<Key> keys) {
        Map<Key, View> result = new HashMap<>();
        var postIds = ids(keys, ReportTargetType.POST);
        var commentIds = ids(keys, ReportTargetType.COMMENT);
        var userIds = ids(keys, ReportTargetType.USER);
        if (!postIds.isEmpty()) {
            posts.findAllById(postIds).forEach(post -> result.put(key(ReportTargetType.POST, post.getId()), postView(post)));
        }
        if (!commentIds.isEmpty()) {
            comments.findAllById(commentIds).forEach(comment ->
                    result.put(key(ReportTargetType.COMMENT, comment.getId()), commentView(comment)));
        }
        if (!userIds.isEmpty()) {
            var profileMap = profiles.findAllById(userIds).stream()
                    .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
            users.findAllById(userIds).forEach(user ->
                    result.put(key(ReportTargetType.USER, user.getId()), userView(user, profileMap.get(user.getId()))));
        }
        return result;
    }

    public static View postView(Post post) {
        return post.getDeletedAt() == null ? contentView(postSnapshot(post), actionId(post.getActiveModerationAction()))
                : View.unavailable();
    }

    public static View commentView(Comment comment) {
        return comment.getDeletedAt() == null ? contentView(commentSnapshot(comment), actionId(comment.getActiveModerationAction()))
                : View.unavailable();
    }

    public static View userView(User user, UserProfile profile) {
        if (!user.isActive()) return View.unavailable();
        boolean suspended = user.isSuspendedAt(Instant.now());
        return new View(userSnapshot(user, profile), suspended ? TargetState.SUSPENDED : TargetState.AVAILABLE,
                suspended ? actionId(user.getActiveModerationAction()) : null,
                suspended ? user.getSuspendedUntil() : null, user.getRole());
    }

    static Long actionId(ModerationAction action) {
        if (action == null) return null;
        var proxy = HibernateProxy.extractLazyInitializer(action);
        return proxy == null ? action.getId() : (Long) proxy.getInternalIdentifier();
    }

    private void validateDiscussion(Comment comment, JwtPayload viewer) {
        if (comment.getTargetType() == null || comment.getTargetId() == null) throw missing();
        try {
            var type = InteractionTargetType.valueOf(comment.getTargetType());
            switch (type) {
                case POST -> visiblePost(comment.getTargetId(), viewer);
                case QUESTION -> {
                    var question = questions.findByIdFetchSessionAndCourseClass(comment.getTargetId())
                            .orElseThrow(ReportTargets::missing);
                    if (question.getSession() == null || !question.getSession().isPracticeVisibleTo(viewer.getUserId())) {
                        throw missing();
                    }
                }
                case SESSION -> discussions.validateTargetExists(type, comment.getTargetId());
                default -> throw missing();
            }
        } catch (AppException | IllegalArgumentException failure) {
            throw missing();
        }
    }

    private Post visiblePost(Long id, JwtPayload viewer) {
        return posts.findReportablePost(id, viewer.getUserId(), UserRole.ADMIN.name().equals(viewer.getRole()))
                .orElseThrow(ReportTargets::missing);
    }

    private static View contentView(ReportSnapshot snapshot, Long actionId) {
        return new View(snapshot, actionId == null ? TargetState.AVAILABLE : TargetState.HIDDEN, actionId, null, null);
    }

    private static ReportSnapshot postSnapshot(Post post) {
        String text = post.getLecturerNote() == null ? post.getContent()
                : java.util.Objects.toString(post.getContent(), "") + "\n" + post.getLecturerNote();
        return new ReportSnapshot(post.getPoster() == null ? null : post.getPoster().getId(), clip(text), post.getUpdatedAt());
    }

    private static ReportSnapshot commentSnapshot(Comment comment) {
        return new ReportSnapshot(comment.getUser() == null ? null : comment.getUser().getId(),
                clip(comment.getContent()), comment.getUpdatedAt());
    }

    private static ReportSnapshot userSnapshot(User user, UserProfile profile) {
        String text = profile == null ? null : profile.getFullName();
        if (profile != null && profile.getBio() != null && !profile.getBio().isBlank()) text += "\n" + profile.getBio();
        return new ReportSnapshot(user.getId(), clip(text), null);
    }

    public static String clip(String text) {
        if (text == null || text.codePointCount(0, text.length()) <= SNAPSHOT_LIMIT) return text;
        return text.substring(0, text.offsetByCodePoints(0, SNAPSHOT_LIMIT));
    }

    private static Collection<Long> ids(Collection<Key> keys, ReportTargetType type) {
        return keys.stream().filter(key -> type.name().equals(key.type()) && key.id() != null)
                .map(Key::id).distinct().toList();
    }

    private static Key key(ReportTargetType type, Long id) {
        return new Key(type.name(), id);
    }

    private static AppException missing() {
        return new AppException(ResponseCode.REPORT_TARGET_NOT_FOUND, "Đối tượng báo cáo không khả dụng.");
    }
}
