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
import java.util.stream.Stream;

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
            parentComment = commentRepository.findForModeration(request.getParentCommentId())
                    .filter(item -> item.getDeletedAt() == null && item.getActiveModerationAction() == null)
                    .orElseThrow(
                            () -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận cha"));
            if (!request.getTargetType().name().equals(parentComment.getTargetType())
                    || !Objects.equals(request.getTargetId(), parentComment.getTargetId())) {
                throw new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Bình luận cha thuộc một cuộc thảo luận khác");
            }
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

        return mapToCommentResponseDto(saved, loadProfiles(Stream.of(saved)), 0, null);
    }

    /**
     * Lấy danh sách bình luận (hỗ trợ phân trang con trỏ Cursor-based Pagination).
     * Hệ thống duy trì cả 2 chế độ sắp xếp:
     * - 'newest': xem các phản hồi mới nhất (phù hợp cập nhật nhanh).
     * - 'oldest': đọc luồng thảo luận bài tập theo trình tự thời gian (Chronological),
     *             giúp người học nắm bắt mạch tư duy giải bài từ đầu đến cuối một cách tự nhiên.
     */
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
            Comment parent = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận cha"));
            String parentType = parent.getTargetType();
            Long parentTargetId = parent.getTargetId();
            validateDiscussionAccess(parent);
            if (isOldest) {
                Long cursor = (after != null && after > 0) ? after : 0L;
                commentsList = commentRepository.findByParentCommentIdAndTargetTypeAndTargetIdAndIdGreaterThanOrderByIdAsc(
                        parentCommentId, parentType, parentTargetId, cursor, pageable);
            } else {
                Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;
                commentsList = commentRepository.findByParentCommentIdAndTargetTypeAndTargetIdAndIdLessThanOrderByIdDesc(
                        parentCommentId, parentType, parentTargetId, cursor, pageable);
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

        List<CommentRepository.ReplyPreview> previews = parentCommentId != null || commentsList.isEmpty() ? List.of()
                : commentRepository.findReplyPreviews(commentsList.stream().map(Comment::getId).toList(), MAX_PREVIEW_REPLIES);
        Map<Long, List<CommentRepository.ReplyPreview>> previewsByParent = previews.stream()
                .collect(Collectors.groupingBy(CommentRepository.ReplyPreview::getParentId));
        Map<Long, Comment> repliesById = previews.isEmpty() ? Map.of()
                : commentRepository.findByIdInFetchUser(previews.stream().map(CommentRepository.ReplyPreview::getCommentId).toList())
                        .stream().collect(Collectors.toMap(Comment::getId, reply -> reply));
        Map<Long, UserProfile> profileMap = loadProfiles(Stream.concat(commentsList.stream(), repliesById.values().stream()));
        List<CommentResponseDto> items = commentsList.stream()
                .map(comment -> {
                    var preview = previewsByParent.get(comment.getId());
                    if (preview == null) return mapToCommentResponseDto(comment, profileMap, 0, null);

                    var replies = preview.stream().map(row -> repliesById.get(row.getCommentId()))
                            .filter(Objects::nonNull).map(reply -> mapToCommentResponseDto(reply, profileMap, 0, null)).toList();
                    return mapToCommentResponseDto(comment, profileMap, preview.getFirst().getReplyCount(), replies);
                }).toList();

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

        Comment comment = commentRepository.findForModeration(commentId)
                .filter(item -> item.getDeletedAt() == null && item.getActiveModerationAction() == null)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        if (comment.getUser() == null || !Objects.equals(comment.getUser().getId(), currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền sửa bình luận này");
        }
        validateDiscussionAccess(comment);

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
        return mapToCommentResponseDto(updated, loadProfiles(Stream.of(updated)), 0, null);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String roleStr = payload.getRole();

        Comment comment = commentRepository.findForModeration(commentId)
                .filter(item -> item.getDeletedAt() == null)
                .orElseThrow(() -> new AppException(ResponseCode.RESOURCE_NOT_FOUND, "Không tìm thấy bình luận"));

        boolean isOwner = comment.getUser() != null && Objects.equals(comment.getUser().getId(), currentUserId);
        boolean isAdmin = UserRole.ADMIN.name().equalsIgnoreCase(roleStr);

        if (!isOwner && !isAdmin) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền xóa bình luận này");
        }
        if (!isAdmin) validateDiscussionAccess(comment);

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
        if (comment.getActiveModerationAction() == null && comment.getTargetType() != null && comment.getTargetId() != null) {
            try {
                InteractionTargetType type = InteractionTargetType.valueOf(comment.getTargetType());
                incrementTargetCommentCounter(type, comment.getTargetId(), -1);
            } catch (Exception ignored) {
            }
        }
    }

    // --- Helper Methods ---

    private void validateDiscussionAccess(Comment comment) {
        if (InteractionTargetType.POST.name().equals(comment.getTargetType())) {
            targetValidator.validateTargetExists(InteractionTargetType.POST, comment.getTargetId());
        } else if (InteractionTargetType.SESSION.name().equals(comment.getTargetType())) {
            targetValidator.validateTargetExists(InteractionTargetType.SESSION, comment.getTargetId());
        }
    }

    private Map<Long, UserProfile> loadProfiles(Stream<Comment> comments) {
        List<Long> userIds = comments.filter(comment -> comment.getDeletedAt() == null
                && comment.getActiveModerationAction() == null).map(Comment::getUser)
                .filter(Objects::nonNull).map(User::getId).distinct().toList();
        return userIds.isEmpty() ? Map.of() : userProfileRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, profile -> profile));
    }

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
            int replyCount, List<CommentResponseDto> replies) {
        boolean moderated = comment.getActiveModerationAction() != null && comment.getDeletedAt() == null;
        boolean isDeleted = comment.getDeletedAt() != null || moderated;

        AuthorDto authorDto = null;
        if (!isDeleted && comment.getUser() != null) {
            UserProfile profile = profileMap.get(comment.getUser().getId());
            authorDto = AuthorDto.builder()
                    .userId(comment.getUser().getId())
                    .fullName(profile != null ? profile.getFullName() : comment.getUser().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .build();
        }

        String mediaUrl = null;
        if (!isDeleted && comment.getMediaUrls() != null && !comment.getMediaUrls().isEmpty()) {
            MediaItem first = comment.getMediaUrls().getFirst();
            mediaUrl = first.url() != null ? first.url() : first.key();
        }

        String contentStr = moderated ? "Bình luận đã bị ẩn do kiểm duyệt"
                : isDeleted ? "Bình luận này đã bị xóa" : comment.getContent();

        return CommentResponseDto.builder()
                .commentId(comment.getId())
                .targetType(comment.getTargetType())
                .targetId(comment.getTargetId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .content(contentStr)
                .mediaUrl(mediaUrl)
                .author(authorDto)
                .createdAt(comment.getCreatedAt())
                .deleted(isDeleted)
                .replyCount(replyCount)
                .replies(replies)
                .build();
    }
}
