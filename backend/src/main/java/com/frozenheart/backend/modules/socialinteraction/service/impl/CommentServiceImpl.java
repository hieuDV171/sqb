package com.frozenheart.backend.modules.socialinteraction.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.media.MediaType;
import com.frozenheart.backend.core.entity.socialinteraction.Comment;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.dto.event.MediaCleanupEvent;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.post.dto.AuthorDto;
import com.frozenheart.backend.modules.socialinteraction.dto.*;
import com.frozenheart.backend.modules.socialinteraction.repository.CommentRepository;
import com.frozenheart.backend.modules.socialinteraction.service.CommentService;
import com.frozenheart.backend.modules.socialinteraction.service.helper.InteractionTargetValidator;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import com.frozenheart.backend.modules.user.service.CounterMetricsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final InteractionTargetValidator targetValidator;
    private final MediaService mediaService;
    private final CounterMetricsService counterMetricsService;
    private final ApplicationEventPublisher eventPublisher;

    private static final int MAX_PREVIEW_REPLIES = 3;

    @Override
    @Transactional
    public CommentResponseDto createComment(CreateCommentRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasMedia = request.getMediaUrl() != null && !request.getMediaUrl().isBlank();

        if (!hasContent && !hasMedia) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER,
                    "Bình luận phải có nội dung chữ hoặc hình ảnh đính kèm");
        }

        targetValidator.validateTargetExists(request.getTargetType(), request.getTargetId());

        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findByIdAndDeletedAtIsNull(request.getParentCommentId())
                    .orElseThrow(
                            () -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận cha"));
        }

        List<MediaItem> mediaItems = null;
        if (hasMedia) {
            mediaService.confirmMediaPermanent(List.of(request.getMediaUrl()));
            mediaItems = List.of(new MediaItem(
                    request.getMediaUrl(), request.getMediaUrl(), MediaType.IMAGE,
                    null, null, null, null, null, null));
        }

        Instant now = Instant.now();
        Comment comment = Comment.builder()
                .targetType(request.getTargetType().name())
                .targetId(request.getTargetId())
                .content(request.getContent())
                .mediaUrls(mediaItems)
                .parentComment(parentComment)
                .user(currentUser)
                .createdAt(now)
                .updatedAt(now)
                .build();

        Comment saved = commentRepository.save(comment);

        // Increment target comment counter
        incrementTargetCommentCounter(request.getTargetType(), request.getTargetId(), 1);

        return mapToCommentResponseDto(saved, Map.of(), false);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<CommentResponseDto> getComments(
            InteractionTargetType targetType, Long targetId, Long parentCommentId, String sort, Long after,
            Integer limit) {

        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 20;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        boolean isOldest = "oldest".equalsIgnoreCase(sort);

        List<Comment> commentsList;

        if (parentCommentId != null) {
            if (isOldest) {
                Long cursor = (after != null && after > 0) ? after : 0L;
                commentsList = commentRepository.findByParentCommentIdAndIdGreaterThanOrderByIdAsc(
                        parentCommentId, cursor, pageable);
            } else {
                Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;
                commentsList = commentRepository.findByParentCommentIdAndIdLessThanOrderByIdDesc(
                        parentCommentId, cursor, pageable);
            }
        } else {
            if (targetType == null || targetId == null) {
                throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER,
                        "Vui lòng cung cấp targetType và targetId");
            }
            targetValidator.validateTargetExists(targetType, targetId);

            if (isOldest) {
                Long cursor = (after != null && after > 0) ? after : 0L;
                commentsList = commentRepository
                        .findByTargetTypeAndTargetIdAndParentCommentIsNullAndIdGreaterThanOrderByIdAsc(
                                targetType.name(), targetId, cursor, pageable);
            } else {
                Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;
                commentsList = commentRepository
                        .findByTargetTypeAndTargetIdAndParentCommentIsNullAndIdLessThanOrderByIdDesc(
                                targetType.name(), targetId, cursor, pageable);
            }
        }

        boolean hasNext = false;
        Long nextCursor = null;

        if (commentsList.size() > pageSize) {
            hasNext = true;
            commentsList = commentsList.subList(0, pageSize);
            nextCursor = commentsList.getLast().getId();
        }

        // Collect all user IDs for profile batch fetch
        List<Long> userIds = commentsList.stream()
                .map(Comment::getUser)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = userIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(userIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        // Map to DTOs
        boolean includeRepliesPreview = (parentCommentId == null);
        List<CommentResponseDto> items = commentsList.stream()
                .map(c -> mapToCommentResponseDto(c, profileMap, includeRepliesPreview))
                .collect(Collectors.toList());

        return CursorResponse.<CommentResponseDto>builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional
    public CommentResponseDto updateComment(Long commentId, UpdateCommentRequest request) {
        boolean hasContent = request.getContent() != null && !request.getContent().isBlank();
        boolean hasMedia = request.getMediaUrl() != null && !request.getMediaUrl().isBlank();

        if (!hasContent && !hasMedia) {
            throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER,
                    "Bình luận phải có nội dung chữ hoặc hình ảnh đính kèm");
        }

        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        if (comment.getUser() == null || !Objects.equals(comment.getUser().getId(), currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền sửa bình luận này");
        }

        List<String> oldMediaKeys = (comment.getMediaUrls() != null)
                ? comment.getMediaUrls().stream().map(MediaItem::url).filter(Objects::nonNull).toList()
                : Collections.emptyList();

        List<String> mediaToDelete = new ArrayList<>();

        if (hasMedia) {
            mediaService.confirmMediaPermanent(List.of(request.getMediaUrl()));
            comment.setMediaUrls(List.of(new MediaItem(
                    request.getMediaUrl(), request.getMediaUrl(), MediaType.IMAGE,
                    null, null, null, null, null, null)));
            for (String oldKey : oldMediaKeys) {
                if (!oldKey.equals(request.getMediaUrl())) {
                    mediaToDelete.add(oldKey);
                }
            }
        } else if (request.getMediaUrl() != null && request.getMediaUrl().isBlank()) {
            comment.setMediaUrls(null);
            mediaToDelete.addAll(oldMediaKeys);
        }

        if (!mediaToDelete.isEmpty()) {
            eventPublisher.publishEvent(MediaCleanupEvent.of(mediaToDelete));
        }

        if (hasContent) {
            comment.setContent(request.getContent());
        }
        comment.setUpdatedAt(Instant.now());

        Comment updated = commentRepository.save(comment);
        return mapToCommentResponseDto(updated, Map.of(), false);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String roleStr = payload.getRole();

        Comment comment = commentRepository.findByIdAndDeletedAtIsNull(commentId)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        boolean isOwner = comment.getUser() != null && Objects.equals(comment.getUser().getId(), currentUserId);
        boolean isAdmin = UserRole.ADMIN.name().equalsIgnoreCase(roleStr);

        if (!isOwner && !isAdmin) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền xóa bình luận này");
        }

        List<String> mediaToDelete = (comment.getMediaUrls() != null)
                ? comment.getMediaUrls().stream().map(MediaItem::url).filter(Objects::nonNull).toList()
                : Collections.emptyList();

        comment.setDeletedAt(Instant.now());
        comment.setContent("Bình luận này đã bị xóa");
        comment.setMediaUrls(null);

        commentRepository.save(comment);

        if (!mediaToDelete.isEmpty()) {
            eventPublisher.publishEvent(MediaCleanupEvent.of(mediaToDelete));
        }

        // Decrement target comment counter
        if (comment.getTargetType() != null && comment.getTargetId() != null) {
            try {
                InteractionTargetType type = InteractionTargetType.valueOf(comment.getTargetType());
                incrementTargetCommentCounter(type, comment.getTargetId(), -1);
            } catch (Exception ignored) {
            }
        }
    }

    // --- Helper Methods ---

    private void incrementTargetCommentCounter(InteractionTargetType targetType, Long targetId, int delta) {
        if (targetType == null || targetId == null)
            return;
        switch (targetType) {
            case POST -> counterMetricsService.incrementPostComments(targetId, delta);
            case SESSION -> counterMetricsService.incrementSessionComments(targetId, delta);
            case QUESTION -> counterMetricsService.incrementQuestionComments(targetId, delta);
        }
    }

    private CommentResponseDto mapToCommentResponseDto(Comment comment, Map<Long, UserProfile> profileMap,
            boolean includeRepliesPreview) {
        boolean hidden = comment.getDeletedAt() != null;

        AuthorDto authorDto = null;
        if (!hidden && comment.getUser() != null) {
            UserProfile profile = profileMap.get(comment.getUser().getId());
            if (profile == null) {
                profile = userProfileRepository.findByUserId(comment.getUser().getId()).orElse(null);
            }
            authorDto = AuthorDto.builder()
                    .id(comment.getUser().getId())
                    .fullName(profile != null ? profile.getFullName() : comment.getUser().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .build();
        }

        String mediaUrl = null;
        if (!hidden && comment.getMediaUrls() != null && !comment.getMediaUrls().isEmpty()) {
            MediaItem first = comment.getMediaUrls().getFirst();
            mediaUrl = first.url() != null ? first.url() : first.key();
        }

        String contentStr = hidden ? "Bình luận này đã bị xóa" : comment.getContent();

        int replyCount = 0;
        List<CommentResponseDto> repliesList = null;

        if (includeRepliesPreview) {
            replyCount = commentRepository.countByParentCommentIdAndDeletedAtIsNull(comment.getId());
            if (replyCount > 0) {
                Pageable previewPageable = PageRequest.of(0, MAX_PREVIEW_REPLIES);
                List<Comment> childReplies = commentRepository.findByParentCommentIdAndIdLessThanOrderByIdDesc(
                        comment.getId(), Long.MAX_VALUE, previewPageable);

                List<Long> replyUserIds = childReplies.stream()
                        .map(Comment::getUser)
                        .filter(Objects::nonNull)
                        .map(User::getId)
                        .distinct()
                        .collect(Collectors.toList());

                Map<Long, UserProfile> replyProfileMap = replyUserIds.isEmpty() ? Map.of()
                        : userProfileRepository.findAllById(replyUserIds).stream()
                                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

                repliesList = childReplies.stream()
                        .map(r -> mapToCommentResponseDto(r, replyProfileMap, false))
                        .collect(Collectors.toList());
            }
        }

        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(contentStr)
                .mediaUrl(mediaUrl)
                .author(authorDto)
                .createdAt(comment.getCreatedAt())
                .hidden(hidden)
                .replyCount(replyCount)
                .replies(repliesList)
                .build();
    }
}
