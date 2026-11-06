import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { reactService, type ReactRequest } from '../services/reactService';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto } from '../types/feed.types';
import type { PostResponseDto } from '@/types/post.types';
import { toast } from '@/stores/useToastStore';

type SupportedReaction = 'LIKE' | 'LOVE' | 'WOW';

function getNextReactionState(
  currentReaction: SupportedReaction | null,
  requestedReaction: SupportedReaction
) {
  if (currentReaction === requestedReaction) {
    return { myReaction: null, reactedByMe: false, countDelta: -1 } as const;
  }
  if (currentReaction) {
    return { myReaction: requestedReaction, reactedByMe: true, countDelta: 0 } as const;
  }
  return { myReaction: requestedReaction, reactedByMe: true, countDelta: 1 } as const;
}

export function usePostReact() {
  const queryClient = useQueryClient();

  const toggleReactMutation = useMutation({
    mutationFn: (data: ReactRequest) => reactService.toggleReact(data),
    onMutate: async (data: ReactRequest) => {
      // 1. Dừng các query activity-feeds đang chạy để tránh ghi đè dữ liệu cũ
      await queryClient.cancelQueries({ queryKey: ['activity-feeds'] });
      await queryClient.cancelQueries({ queryKey: ['post-detail', data.targetId] });

      // 2. Chụp snapshot cache hiện tại làm điểm phục hồi nếu có lỗi
      const previousFeeds = queryClient.getQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>({
        queryKey: ['activity-feeds'],
      });
      const previousPostDetail = queryClient.getQueryData<PostResponseDto>([
        'post-detail',
        data.targetId,
      ]);

      // 3. Optimistic Update: Phản hồi tức thì 0ms (Zero Latency) đa hình cho 3 loại cảm xúc LIKE, LOVE, WOW
      queryClient.setQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>(
        { queryKey: ['activity-feeds'] },
        (oldData) => {
          if (!oldData) return oldData;

          return {
            ...oldData,
            pages: oldData.pages.map((page) => {
              if (!page?.items) return page;

              return {
                ...page,
                items: page.items.map((item) => {
                  if (item.targetType === data.targetType && item.targetId === data.targetId) {
                    const currentReaction =
                      item.content.myReaction || (item.content.reactedByMe ? 'LIKE' : null);
                    const nextState = getNextReactionState(
                      currentReaction,
                      data.reactionType
                    );

                    return {
                      ...item,
                      content: {
                        ...item.content,
                        reactedByMe: nextState.reactedByMe,
                        myReaction: nextState.myReaction,
                        reactCount: Math.max(
                          0,
                          (item.content.reactCount || 0) + nextState.countDelta
                        ),
                      },
                    };
                  }
                  return item;
                }),
              };
            }),
          };
        }
      );

      queryClient.setQueryData<PostResponseDto>(['post-detail', data.targetId], (current) => {
        if (!current) return current;

        const currentReaction = current.myReaction || (current.reactedByMe ? 'LIKE' : null);
        const nextState = getNextReactionState(currentReaction, data.reactionType);
        return {
          ...current,
          reactedByMe: nextState.reactedByMe,
          myReaction: nextState.myReaction,
          reactCount: Math.max(0, current.reactCount + nextState.countDelta),
        };
      });

      return { previousFeeds, previousPostDetail };
    },
    onError: (_err, data, context) => {
      // 4. Rollback phục hồi lại cache cũ nếu server trả về lỗi hoặc mất mạng
      if (context?.previousFeeds) {
        for (const [queryKey, data] of context.previousFeeds) {
          queryClient.setQueryData(queryKey, data);
        }
      }
      if (context?.previousPostDetail) {
        queryClient.setQueryData(
          ['post-detail', data.targetId],
          context.previousPostDetail
        );
      }
      toast.error('Không thể cập nhật cảm xúc. Vui lòng kiểm tra kết nối mạng!');
    },
    onSuccess: (res, data) => {
      if (data.targetType === 'POST') {
        void queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      }
      // 5. Cập nhật dữ liệu chuẩn xác từ Server (Reconciliation)
      if (res?.data) {
        const { myReaction, totalCount, reactionCounts } = res.data;
        queryClient.setQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>(
          { queryKey: ['activity-feeds'] },
          (oldData) => {
            if (!oldData) return oldData;
            return {
              ...oldData,
              pages: oldData.pages.map((page) => ({
                ...page,
                items: page.items.map((item) => {
                  if (item.targetType === data.targetType && item.targetId === data.targetId) {
                    return {
                      ...item,
                      content: {
                        ...item.content,
                        reactedByMe: myReaction !== null && myReaction !== undefined,
                        myReaction: myReaction as SupportedReaction | null,
                        reactCount: totalCount ?? item.content.reactCount,
                        reactionCounts: reactionCounts || item.content.reactionCounts,
                      },
                    };
                  }
                  return item;
                }),
              })),
            };
          }
        );
        queryClient.setQueryData<PostResponseDto>(['post-detail', data.targetId], (current) =>
          current
            ? {
                ...current,
                reactedByMe: myReaction !== null && myReaction !== undefined,
                myReaction: myReaction as SupportedReaction | null,
                reactCount: totalCount ?? current.reactCount,
              }
            : current
        );
      }
    },
  });

  return {
    toggleReact: toggleReactMutation.mutateAsync,
    isReacting: toggleReactMutation.isPending,
  };
}
