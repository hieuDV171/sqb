package com.frozenheart.backend.modules.post.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.modules.post.dto.*;
import com.frozenheart.backend.modules.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping("/posts")
    public ResponseEntity<GlobalResponse<PostResponseDto>> createPost(
            @Valid @RequestBody CreatePostRequest request) {
        PostResponseDto response = postService.createPost(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/posts/me")
    public ResponseEntity<GlobalResponse<CursorResponse<PostResponseDto>>> getMyPosts(
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        CursorResponse<PostResponseDto> response = postService.getMyPosts(after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/users/{userId}/posts")
    public ResponseEntity<GlobalResponse<CursorResponse<PostResponseDto>>> getUserPosts(
            @PathVariable Long userId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        CursorResponse<PostResponseDto> response = postService.getUserPosts(userId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<PostResponseDto>> getPost(@PathVariable Long id) {
        PostResponseDto response = postService.getPostById(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<UpdatePostResponseDto>> updatePost(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePostRequest request) {
        UpdatePostResponseDto response = postService.updatePost(id, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/posts/{id}/lecturer-notes")
    public ResponseEntity<GlobalResponse<Void>> updateLecturerNote(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLecturerNoteRequest request) {
        postService.updateLecturerNote(id, request);
        return ResponseEntity.ok(GlobalResponse.success());
    }

    @DeleteMapping("/posts/{id}")
    public ResponseEntity<GlobalResponse<DeletePostResponseDto>> deletePost(@PathVariable Long id) {
        DeletePostResponseDto response = postService.deletePost(id);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/video-posts")
    public ResponseEntity<GlobalResponse<CursorResponse<VideoPostItemDto>>> getVideoPosts(
            @RequestParam(name = "subject_id", required = false) Long subjectId,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "2") Integer limit) {
        CursorResponse<VideoPostItemDto> response = postService.getVideoPosts(subjectId, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }
}
