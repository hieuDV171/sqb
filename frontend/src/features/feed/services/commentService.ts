import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CursorResponse } from '@/types/user.types';
import type { CommentResponseDto } from '@/types/post.types';

export interface CreateCommentRequest {
  targetType: 'POST' | 'COMMENT' | 'QUESTION' | 'SESSION';
  targetId: number;
  content: string;
  parentCommentId?: number;
  mediaUrl?: string;
}

export interface UpdateCommentRequest {
  content: string;
  mediaUrl?: string;
}

export const commentService = {
  /**
   * Tạo bình luận mới cho bài viết hoặc trả lời một bình luận khác
   * POST /api/v1/comments
   */
  createComment: async (data: CreateCommentRequest): Promise<GlobalResponse<CommentResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<CommentResponseDto>>('/comments', data)) as any;
  },

  /**
   * Lấy danh sách bình luận (Cursor Pagination)
   * GET /api/v1/comments
   */
  getComments: async (params: {
    targetType?: 'POST' | 'COMMENT' | 'QUESTION' | 'SESSION';
    targetId?: number;
    parentCommentId?: number;
    sort?: 'newest' | 'oldest';
    after?: number;
    limit?: number;
  }): Promise<GlobalResponse<CursorResponse<CommentResponseDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<CommentResponseDto>>>('/comments', {
      params: {
        target_type: params.targetType,
        target_id: params.targetId,
        parent_comment_id: params.parentCommentId,
        sort: params.sort || 'newest',
        after: params.after,
        limit: params.limit || 20,
      },
    })) as any;
  },

  /**
   * Cập nhật nội dung bình luận
   * PUT /api/v1/comments/{commentId}
   */
  updateComment: async (
    commentId: number,
    data: UpdateCommentRequest
  ): Promise<GlobalResponse<CommentResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<CommentResponseDto>>(
      `/comments/${commentId}`,
      data
    )) as any;
  },

  /**
   * Xóa bình luận
   * DELETE /api/v1/comments/{commentId}
   */
  deleteComment: async (commentId: number): Promise<GlobalResponse<void>> => {
    return (await axiosClient.delete<GlobalResponse<void>>(`/comments/${commentId}`)) as any;
  },
};
