package com.frozenheart.backend.modules.socialinteraction.controller;

import com.frozenheart.backend.core.dto.GlobalResponse;
import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.modules.socialinteraction.dto.*;
import com.frozenheart.backend.modules.socialinteraction.service.CommentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping("/comments")
    public ResponseEntity<GlobalResponse<CommentResponseDto>> createComment(
            @Valid @RequestBody CreateCommentRequest request) {
        CommentResponseDto response = commentService.createComment(request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @GetMapping("/comments")
    public ResponseEntity<GlobalResponse<CursorResponse<CommentResponseDto>>> getComments(
            @RequestParam(name = "target_type", required = false) InteractionTargetType targetType,
            @RequestParam(name = "target_id", required = false) Long targetId,
            @RequestParam(name = "parent_comment_id", required = false) Long parentCommentId,
            @RequestParam(required = false, defaultValue = "newest") String sort,
            @RequestParam(required = false) Long after,
            @RequestParam(required = false, defaultValue = "20") Integer limit) {
        CursorResponse<CommentResponseDto> response = commentService.getComments(
                targetType, targetId, parentCommentId, sort, after, limit);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @PutMapping("/comments/{commentId}")
    public ResponseEntity<GlobalResponse<CommentResponseDto>> updateComment(
            @PathVariable Long commentId,
            @Valid @RequestBody UpdateCommentRequest request) {
        CommentResponseDto response = commentService.updateComment(commentId, request);
        return ResponseEntity.ok(GlobalResponse.success(response));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<GlobalResponse<Void>> deleteComment(@PathVariable Long commentId) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok(GlobalResponse.success());
    }
}
