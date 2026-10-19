import { useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { reactService, type ReactRequest } from '../services/reactService';
import type { CursorResponse } from '@/types/user.types';
import type { ActivityFeedItemDto } from '../types/feed.types';
import { toast } from '@/stores/useToastStore';

export function usePostReact() {
  const queryClient = useQueryClient();

  const toggleReactMutation = useMutation({
    mutationFn: (data: ReactRequest) => reactService.toggleReact(data),
    onMutate: async (data: ReactRequest) => {
      // 1. Dừng các query activity-feeds đang chạy để tránh ghi đè dữ liệu cũ
      await queryClient.cancelQueries({ queryKey: ['activity-feeds'] });

      // 2. Chụp snapshot cache hiện tại làm điểm phục hồi nếu có lỗi
      const previousFeeds = queryClient.getQueriesData<InfiniteData<CursorResponse<ActivityFeedItemDto>>>({
        queryKey: ['activity-feeds'],
      });

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
                    const currentReaction = item.content.myReaction || (item.content.reactedByMe ? 'LIKE' : null);
                    const isSameType = currentReaction === data.reactionType;

                    let nextMyReaction: 'LIKE' | 'LOVE' | 'WOW' | null = null;
                    let nextReacted = false;
                    let deltaCount = 0;

                    if (isSameType) {
                      // Hủy cảm xúc nếu bấm lại cùng loại
                      nextMyReaction = null;
                      nextReacted = false;
                      deltaCount = -1;
                    } else if (currentReaction) {
                      // Đổi loại cảm xúc khác (ví dụ từ LIKE sang LOVE): Tổng số lượng giữ nguyên
                      nextMyReaction = data.reactionType;
                      nextReacted = true;
                      deltaCount = 0;
                    } else {
                      // Thả cảm xúc mới
                      nextMyReaction = data.reactionType;
                      nextReacted = true;
                      deltaCount = 1;
                    }

                    return {
                      ...item,
                      content: {
                        ...item.content,
                        reactedByMe: nextReacted,
                        myReaction: nextMyReaction,
                        reactCount: Math.max(0, (item.content.reactCount || 0) + deltaCount),
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

      return { previousFeeds };
    },
    onError: (_err, _data, context) => {
      // 4. Rollback phục hồi lại cache cũ nếu server trả về lỗi hoặc mất mạng
      if (context?.previousFeeds) {
        for (const [queryKey, data] of context.previousFeeds) {
          queryClient.setQueryData(queryKey, data);
        }
      }
      toast.error('Không thể cập nhật cảm xúc. Vui lòng kiểm tra kết nối mạng!');
    },
    onSuccess: (res, data) => {
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
                        myReaction: myReaction as any,
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
      }
    },
  });

  return {
    toggleReact: toggleReactMutation.mutateAsync,
    isReacting: toggleReactMutation.isPending,
  };
}
