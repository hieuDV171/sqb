package com.frozenheart.backend.modules.post.service.impl;

import com.frozenheart.backend.core.constant.ResponseCode;
import com.frozenheart.backend.core.entity.media.MediaItem;
import com.frozenheart.backend.core.entity.post.Post;
import com.frozenheart.backend.core.entity.post.PostType;
import com.frozenheart.backend.core.entity.post.PostVisibility;
import com.frozenheart.backend.core.entity.session.Subject;
import com.frozenheart.backend.core.entity.user.User;
import com.frozenheart.backend.core.entity.user.UserProfile;
import com.frozenheart.backend.core.entity.user.UserRole;
import com.frozenheart.backend.core.exception.AppException;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent;
import com.frozenheart.backend.core.dto.event.EntitySearchSyncEvent.EntityType;
import com.frozenheart.backend.core.dto.jwt.JwtPayload;
import com.frozenheart.backend.core.dto.pagination.CursorPaginationDto;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.activityfeed.ActionType;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedMetaData;
import com.frozenheart.backend.core.entity.activityfeed.ActivityFeedTargetType;
import com.frozenheart.backend.modules.activityfeed.service.ActivityFeedService;
import com.frozenheart.backend.modules.media.service.MediaService;
import com.frozenheart.backend.modules.post.dto.*;
import com.frozenheart.backend.modules.post.repository.PostRepository;
import com.frozenheart.backend.modules.post.service.PostService;
import com.frozenheart.backend.modules.session.repository.SubjectRepository;
import com.frozenheart.backend.modules.user.repository.UserProfileRepository;
import com.frozenheart.backend.modules.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final SubjectRepository subjectRepository;
    private final MediaService mediaService;
    private final ActivityFeedService activityFeedService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public PostResponseDto createPost(CreatePostRequest request) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String roleStr = payload.getRole();

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        PostType postType = request.getPostType();
        Subject subject = null;

        if (request.getSubjectId() != null || postType == PostType.LEARNING_VIDEO) {
            if (!isLecturerOrAdmin(roleStr)) {
                throw new AppException(ResponseCode.ACCESS_DENIED, "Chỉ Giảng viên hoặc Admin mới có quyền đăng video bài giảng");
            }
            if (request.getSubjectId() == null) {
                throw new AppException(ResponseCode.MISSING_REQUIRED_PARAMETER, "Vui lòng chọn môn học cho video bài giảng");
            }
            subject = subjectRepository.findById(request.getSubjectId())
                    .orElseThrow(() -> new AppException(ResponseCode.INVALID_PARAMETER_VALUE, "Môn học không tồn tại"));
            postType = PostType.LEARNING_VIDEO;
        } else {
            postType = PostType.SOCIAL_POST;
        }

        // Confirm media permanent
        confirmMediaItemsPermanent(request.getMediaUrls());

        LocalDateTime now = LocalDateTime.now();

        Post post = new Post();
        post.setContent(request.getContent());
        post.setPostType(postType);
        post.setMediaUrls(request.getMediaUrls());
        post.setVisibility(request.getVisibility() != null ? request.getVisibility() : PostVisibility.PUBLIC);
        post.setPoster(currentUser);
        post.setSubject(subject);
        post.setReactCount(0);
        post.setCommentCount(0);
        post.setCreatedAt(now);
        post.setUpdatedAt(now);

        Post savedPost = postRepository.save(post);
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.POST, savedPost.getId()));

        ActionType feedAction = (postType == PostType.LEARNING_VIDEO) ? ActionType.PUBLISHED_LECTURE_VIDEO : ActionType.CREATED_POST;
        String firstMediaUrl = (request.getMediaUrls() != null && !request.getMediaUrls().isEmpty()) ? request.getMediaUrls().get(0).url() : null;
        ActivityFeedMetaData feedMeta = ActivityFeedMetaData.builder()
                .title(request.getContent())
                .mediaUrl(firstMediaUrl)
                .subjectCode(subject != null ? subject.getCode() : null)
                .subjectName(subject != null ? subject.getName() : null)
                .build();

        // TODO: LogActivity
        activityFeedService.logActivity(currentUser, feedAction, ActivityFeedTargetType.POST.name(), savedPost.getId(), feedMeta);

        return mapToPostResponseDto(savedPost);
    }

    @Override
    @Transactional(readOnly = true)
    public PostResponseDto getPostById(Long id) {
        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ResponseCode.POST_NOT_FOUND));

        // TODO: [PRIVACY_MODULE] When friendship, follow, and block modules (5.4.1, 5.4.2, 5.4.3) are implemented, verify if requester is blocked by author or if visibility (PUBLIC, FRIENDS, ONLY_ME) permits access.

        return mapToPostResponseDto(post);
    }

    @Override
    @Transactional
    public UpdatePostResponseDto updatePost(Long id, UpdatePostRequest request) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();

        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ResponseCode.POST_NOT_FOUND));

        if (post.getPoster() == null || !Objects.equals(post.getPoster().getId(), currentUserId)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Bạn không có quyền sửa bài viết này");
        }

        // Confirm new media permanent
        confirmMediaItemsPermanent(request.getMediaUrls());
        // TODO: [MEDIA_CLEANUP] When updating mediaUrls, old media keys removed from the post become orphan objects in storage. Implement reconciliation cronjob or call mediaService.deleteMedia(removedKeys).

        post.setContent(request.getContent());
        post.setMediaUrls(request.getMediaUrls());
        if (request.getVisibility() != null) {
            post.setVisibility(request.getVisibility());
        }
        post.setUpdatedAt(LocalDateTime.now());

        Post updated = postRepository.save(post);
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntitySearchSyncEvent.EntityType.POST, updated.getId()));

        return UpdatePostResponseDto.builder()
                .postId(updated.getId())
                .updatedAt(updated.getUpdatedAt())
                .build();
    }

    @Override
    @Transactional
    public void updateLecturerNote(Long id, UpdateLecturerNoteRequest request) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String roleStr = payload.getRole();

        if (!isLecturerOrAdmin(roleStr)) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Chỉ Giảng viên hoặc Admin mới có quyền thêm ghi chú");
        }

        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ResponseCode.POST_NOT_FOUND));

        User currentLecturer = userRepository.findById(currentUserId)
                .orElseThrow(() -> new AppException(ResponseCode.USER_NOT_FOUND));

        post.setLecturerNote(request.getContent());
        post.setLecturerNoteAddedAt(LocalDateTime.now());
        post.setNotedLecturer(currentLecturer);

        Post savedPost = postRepository.save(post);
        eventPublisher.publishEvent(EntitySearchSyncEvent.upsert(EntityType.POST, savedPost.getId()));
    }

    @Override
    @Transactional
    public void deletePost(Long id) {
        JwtPayload payload = JwtPayload.getCurrentUserPayload();
        Long currentUserId = payload.getUserId();
        String roleStr = payload.getRole();

        Post post = postRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new AppException(ResponseCode.POST_NOT_FOUND));

        boolean isOwner = post.getPoster() != null && Objects.equals(post.getPoster().getId(), currentUserId);
        boolean isAdmin = UserRole.ADMIN.name().equalsIgnoreCase(roleStr);

        if (!isOwner && !isAdmin) {
            throw new AppException(ResponseCode.ACCESS_DENIED, "Chỉ tác giả bài viết hoặc Admin mới có quyền xóa bài viết");
        }

        post.setDeletedAt(LocalDateTime.now());
        post.setContent("Bài viết này đã bị xóa");
        post.setMediaUrls(null);

        Post savedPost = postRepository.save(post);
        eventPublisher.publishEvent(EntitySearchSyncEvent.delete(EntitySearchSyncEvent.EntityType.POST, savedPost.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<VideoPostItemDto> getVideoPosts(Long subjectId, Long after, Integer limit) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 3) : 1;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;

        List<Post> postsList;
        if (subjectId != null) {
            postsList = postRepository.findByPostTypeAndSubjectIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                    PostType.LEARNING_VIDEO, subjectId, cursor, pageable);
        } else {
            postsList = postRepository.findByPostTypeAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                    PostType.LEARNING_VIDEO, cursor, pageable);
        }

        boolean hasNext = false;
        Long nextCursor = null;

        if (postsList.size() > pageSize) {
            hasNext = true;
            postsList = postsList.subList(0, pageSize);
            nextCursor = postsList.getLast().getId();
        }

        List<Long> posterIds = postsList.stream()
                .map(Post::getPoster)
                .filter(Objects::nonNull)
                .map(User::getId)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, UserProfile> profileMap = posterIds.isEmpty() ? Map.of()
                : userProfileRepository.findAllById(posterIds).stream()
                        .collect(Collectors.toMap(UserProfile::getUserId, p -> p));

        List<VideoPostItemDto> items = postsList.stream()
                .map(post -> mapToVideoPostItemDto(post, profileMap))
                .collect(Collectors.toList());

        return CursorResponse.<VideoPostItemDto>builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<PostResponseDto> getMyPosts(Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        return getUserPostsInternal(currentUserId, after, limit, true);
    }

    @Override
    @Transactional(readOnly = true)
    public CursorResponse<PostResponseDto> getUserPosts(Long userId, Long after, Integer limit) {
        Long currentUserId = JwtPayload.getCurrentUserPayload().getUserId();
        boolean isSelf = Objects.equals(currentUserId, userId);
        return getUserPostsInternal(userId, after, limit, isSelf);
    }

    private CursorResponse<PostResponseDto> getUserPostsInternal(Long userId, Long after, Integer limit, boolean isSelf) {
        int pageSize = (limit != null && limit > 0) ? Math.min(limit, 50) : 10;
        Pageable pageable = PageRequest.of(0, pageSize + 1);
        Long cursor = (after != null && after > 0) ? after : Long.MAX_VALUE;

        List<Post> postsList = postRepository.findByPosterIdAndIdLessThanAndDeletedAtIsNullOrderByIdDesc(
                userId, cursor, pageable);

        boolean hasNext = false;
        Long nextCursor = null;

        if (postsList.size() > pageSize) {
            hasNext = true;
            postsList = postsList.subList(0, pageSize);
            nextCursor = postsList.getLast().getId();
        }

        // TODO: [PRIVACY_MODULE] If !isSelf, filter out posts based on post.visibility (e.g. ONLY_ME or FRIENDS if not friends).

        List<PostResponseDto> items = postsList.stream()
                .map(this::mapToPostResponseDto)
                .collect(Collectors.toList());

        return CursorResponse.<PostResponseDto>builder()
                .items(items)
                .pagination(CursorPaginationDto.builder()
                        .after(nextCursor)
                        .hasNext(hasNext)
                        .build())
                .build();
    }

    // --- Helper Methods ---

    private void confirmMediaItemsPermanent(List<MediaItem> mediaItems) {
        if (mediaItems == null || mediaItems.isEmpty()) return;
        List<String> keys = mediaItems.stream()
                .map(item -> {
                    if (item.key() != null && !item.key().isBlank()) {
                        return item.key();
                    }
                    return item.url();
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (!keys.isEmpty()) {
            mediaService.confirmMediaPermanent(keys);
        }
    }

    private boolean isLecturerOrAdmin(String roleStr) {
        if (roleStr == null) return false;
        return UserRole.LECTURER.name().equalsIgnoreCase(roleStr) || UserRole.ADMIN.name().equalsIgnoreCase(roleStr);
    }

    private PostResponseDto mapToPostResponseDto(Post post) {
        AuthorDto authorDto = mapUserToAuthorDto(post.getPoster());
        AuthorDto notedLecturerDto = mapUserToAuthorDto(post.getNotedLecturer());

        // TODO: [REACTION_MODULE] Check reaction status (likedByMe) when Reaction module (5.3.3) is implemented.

        return PostResponseDto.builder()
                .postId(post.getId())
                .content(post.getContent())
                .postType(post.getPostType())
                .mediaUrls(post.getMediaUrls())
                .visibility(post.getVisibility())
                .author(authorDto)
                .reactCount(post.getReactCount())
                .commentCount(post.getCommentCount())
                .likedByMe(false)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .lecturerNote(post.getLecturerNote())
                .lecturerNoteAddedAt(post.getLecturerNoteAddedAt())
                .notedLecturer(notedLecturerDto)
                .subjectId(post.getSubject() != null ? post.getSubject().getId() : null)
                .subjectName(post.getSubject() != null ? post.getSubject().getName() : null)
                .subjectCode(post.getSubject() != null ? post.getSubject().getCode() : null)
                .sessionId(post.getSession() != null ? post.getSession().getId() : null)
                .build();
    }

    private VideoPostItemDto mapToVideoPostItemDto(Post post, Map<Long, UserProfile> profileMap) {
        AuthorDto authorDto;
        if (post.getPoster() == null) {
            authorDto = AuthorDto.getSystemAuthor();
        } else {
            UserProfile profile = profileMap.get(post.getPoster().getId());
            authorDto = AuthorDto.builder()
                    .id(post.getPoster().getId())
                    .fullName(profile != null ? profile.getFullName() : post.getPoster().getEmail())
                    .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                    .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                    .build();
        }

        return VideoPostItemDto.builder()
                .postId(post.getId())
                .content(post.getContent())
                .visibility(post.getVisibility())
                .reactCount(post.getReactCount())
                .commentCount(post.getCommentCount())
                .mediaUrls(post.getMediaUrls())
                .author(authorDto)
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .subjectId(post.getSubject() != null ? post.getSubject().getId() : null)
                .subjectName(post.getSubject() != null ? post.getSubject().getName() : null)
                .subjectCode(post.getSubject() != null ? post.getSubject().getCode() : null)
                .build();
    }

    private AuthorDto mapUserToAuthorDto(User user) {
        if (user == null) {
            return AuthorDto.getSystemAuthor();
        }
        UserProfile profile = userProfileRepository.findByUserId(user.getId()).orElse(null);
        return AuthorDto.builder()
                .id(user.getId())
                .fullName(profile != null ? profile.getFullName() : user.getEmail())
                .avatarUrl(profile != null ? profile.getAvatarUrl() : null)
                .frameUrl(profile != null ? profile.getAvatarFrameUrl() : null)
                .build();
    }
}
