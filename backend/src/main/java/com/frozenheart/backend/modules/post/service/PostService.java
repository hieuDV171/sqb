package com.frozenheart.backend.modules.post.service;

import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.post.dto.*;

public interface PostService {

    PostResponseDto createPost(CreatePostRequest request);

    PostResponseDto getPostById(Long id);

    UpdatePostResponseDto updatePost(Long id, UpdatePostRequest request);

    void updateLecturerNote(Long id, UpdateLecturerNoteRequest request);

    DeletePostResponseDto deletePost(Long id);

    CursorResponse<VideoPostItemDto> getVideoPosts(Long subjectId, Long after, Integer limit);

    CursorResponse<PostResponseDto> getMyPosts(Long after, Integer limit);

    CursorResponse<PostResponseDto> getUserPosts(Long userId, Long after, Integer limit);
}
