package com.frozenheart.backend.modules.socialinteraction.service;

import com.frozenheart.backend.core.dto.pagination.CursorResponse;
import com.frozenheart.backend.core.entity.socialinteraction.InteractionTargetType;
import com.frozenheart.backend.modules.socialinteraction.dto.*;

public interface CommentService {

    CommentResponseDto createComment(CreateCommentRequest request);

    CursorResponse<CommentResponseDto> getComments(InteractionTargetType targetType, Long targetId, Long parentCommentId, String sort, Long after, Integer limit);

    CommentResponseDto updateComment(Long commentId, UpdateCommentRequest request);

    void deleteComment(Long commentId);
}
