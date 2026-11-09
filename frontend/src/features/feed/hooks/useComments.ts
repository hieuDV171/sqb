import { useInfiniteQuery, useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { commentService, type CreateCommentRequest } from '../services/commentService';
import type { CommentResponseDto, PostResponseDto } from '@/types/post.types';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto } from '../types/feed.types';
import { useToastStore } from '@/stores/useToastStore';

export function useComments(targetType: 'POST' | 'COMMENT' | 'SESSION', targetId: number) {
  const queryClient = useQueryClient();
  const { addToast } = useToastStore();

  const queryKey = ['comments', targetType, targetId];

  const commentsQuery = useInfiniteQuery({
    queryKey,
    queryFn: async ({ pageParam }) => {
      const res = await commentService.getComments({
        targetType,
        targetId,
        sort: 'newest',
        after: pageParam as number | undefined,
        limit: 15,
      });
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage?.pagination?.hasNext && lastPage?.pagination?.after) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    staleTime: 1000 * 20,
  });

  const comments: CommentResponseDto[] =
    commentsQuery.data?.pages.flatMap((page) => page?.items || []) || [];

  const createCommentMutation = useMutation({
    mutationFn: (data: Omit<CreateCommentRequest, 'targetType' | 'targetId'> & { parentCommentId?: number }) =>
      commentService.createComment({
        targetType,
        targetId,
        content: data.content,
        parentCommentId: data.parentCommentId,
        mediaUrl: data.mediaUrl,
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });

      if (targetType === 'SESSION') {
        void queryClient.invalidateQueries({ queryKey: ['userProposedSessions'] });
        void queryClient.invalidateQueries({ queryKey: ['my-submissions'] });
      }
      if (targetType === 'POST') {
        void queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
        queryClient.setQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>(
          { queryKey: ['activity-feeds'] },
          (oldData) => {
            if (!oldData) return oldData;
            return {
              ...oldData,
              pages: oldData.pages.map((page) => ({
                ...page,
                items: page.items.map((item) => {
                  if (item.targetType === 'POST' && item.targetId === targetId) {
                    return {
                      ...item,
                      content: {
                        ...item.content,
                        commentCount: (item.content.commentCount || 0) + 1,
                      },
                    };
                  }
                  return item;
                }),
              })),
            };
          }
        );
        queryClient.setQueryData<PostResponseDto>(['post-detail', targetId], (current) =>
          current ? { ...current, commentCount: current.commentCount + 1 } : current
        );
      }
    },
    onError: (err) => {
      addToast({
        type: 'error',
        title: 'Bình luận thất bại',
        message: err.message || 'Không thể đăng bình luận.',
      });
    },
  });

  const deleteCommentMutation = useMutation({
    mutationFn: (commentId: number) => commentService.deleteComment(commentId),
    onMutate: async (deletedCommentId: number) => {
      // 1. Dừng các query comments đang chạy để tránh ghi đè dữ liệu cũ
      await queryClient.cancelQueries({ queryKey });

      // 2. Chụp snapshot cache hiện tại làm điểm phục hồi nếu có lỗi
      const previousComments = queryClient.getQueryData<InfiniteData<CursorResponse<CommentResponseDto>>>(queryKey);
      const previousPostDetail = targetType === 'POST' ? queryClient.getQueryData<PostResponseDto>([
        'post-detail',
        targetId,
      ]) : undefined;

      // 3. Optimistic Update: Xóa ngay lập tức bình luận khỏi UI (0ms delay)
      if (previousComments) {
        queryClient.setQueryData<InfiniteData<CursorResponse<CommentResponseDto>>>(queryKey, {
          ...previousComments,
          pages: previousComments.pages.map((page) => ({
            ...page,
            items: (page.items || []).filter((c) => c.commentId !== deletedCommentId),
          })),
        });
      }

      // Giảm ngay commentCount trên bài viết trong activity feed (nếu là bình luận của POST)
      if (targetType === 'POST') {
        queryClient.setQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>(
          { queryKey: ['activity-feeds'] },
          (oldData) => {
            if (!oldData) return oldData;
            return {
              ...oldData,
              pages: oldData.pages.map((page) => ({
                ...page,
                items: page.items.map((item) => {
                  if (item.targetType === 'POST' && item.targetId === targetId) {
                    return {
                      ...item,
                      content: {
                        ...item.content,
                        commentCount: Math.max(0, (item.content.commentCount || 0) - 1),
                      },
                    };
                  }
                  return item;
                }),
              })),
            };
          }
        );
        queryClient.setQueryData<PostResponseDto>(['post-detail', targetId], (current) =>
          current
            ? { ...current, commentCount: Math.max(0, current.commentCount - 1) }
            : current
        );
      }

      return { previousComments, previousPostDetail };
    },
    onError: (_err, _commentId, context) => {
      // 4. Rollback lại danh sách bình luận cũ nếu xóa thất bại
      if (context?.previousComments) {
        queryClient.setQueryData(queryKey, context.previousComments);
      }
      if (context?.previousPostDetail) {
        queryClient.setQueryData(['post-detail', targetId], context.previousPostDetail);
      }
      addToast({
        type: 'error',
        title: 'Lỗi',
        message: 'Không thể xóa bình luận này. Vui lòng thử lại!',
      });
    },
    onSuccess: () => {
      if (targetType === 'SESSION') {
        void queryClient.invalidateQueries({ queryKey: ['userProposedSessions'] });
        void queryClient.invalidateQueries({ queryKey: ['my-submissions'] });
      }
      addToast({
        type: 'success',
        title: 'Thành công',
        message: 'Đã xóa bình luận!',
      });
    },
    onSettled: () => {
      // 5. Luôn làm mới ngầm sau cùng để đối soát với server
      queryClient.invalidateQueries({ queryKey });
      if (targetType === 'POST') {
        void queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
        void queryClient.invalidateQueries({ queryKey: ['post-detail', targetId] });
        void queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      }
    },
  });

  return {
    comments,
    isLoading: commentsQuery.isLoading,
    isLoadingError: commentsQuery.isLoadingError,
    isFetchNextPageError: commentsQuery.isFetchNextPageError,
    isFetchingNextPage: commentsQuery.isFetchingNextPage,
    hasNextPage: commentsQuery.hasNextPage,
    fetchNextPage: commentsQuery.fetchNextPage,
    createComment: createCommentMutation.mutateAsync,
    isCreating: createCommentMutation.isPending,
    deleteComment: deleteCommentMutation.mutate,
    isDeleting: deleteCommentMutation.isPending,
    refetch: commentsQuery.refetch,
  };
}
