import { useInfiniteQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { commentService, type CreateCommentRequest } from '../services/commentService';
import type { CommentResponseDto } from '@/types/post.types';
import { useToastStore } from '@/stores/useToastStore';

export function useComments(targetType: 'POST' | 'COMMENT', targetId: number) {
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
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
    },
    onError: (err: any) => {
      addToast({
        type: 'error',
        title: 'Bình luận thất bại',
        message: err?.response?.data?.message || 'Không thể đăng bình luận.',
      });
    },
  });

  const deleteCommentMutation = useMutation({
    mutationFn: (commentId: number) => commentService.deleteComment(commentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey });
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
      addToast({
        type: 'success',
        title: 'Thành công',
        message: 'Đã xóa bình luận!',
      });
    },
    onError: () => {
      addToast({
        type: 'error',
        title: 'Lỗi',
        message: 'Không thể xóa bình luận này.',
      });
    },
  });

  return {
    comments,
    isLoading: commentsQuery.isLoading,
    isFetchingNextPage: commentsQuery.isFetchingNextPage,
    hasNextPage: commentsQuery.hasNextPage,
    fetchNextPage: commentsQuery.fetchNextPage,
    createComment: createCommentMutation.mutateAsync,
    isCreating: createCommentMutation.isPending,
    deleteComment: deleteCommentMutation.mutateAsync,
    isDeleting: deleteCommentMutation.isPending,
    refetch: commentsQuery.refetch,
  };
}
