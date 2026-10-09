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

      // 3. Optimistic Update: Phản hồi tức thì 0ms (Zero Latency)
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
                    const currentlyReacted = Boolean(item.content.reactedByMe);

                    // Thuật toán cho hệ thống cảm xúc:
                    // - Nếu đang react mà bấm lại -> Hủy (reactedByMe = false, count - 1)
                    // - Nếu chưa react mà bấm -> Thả mới (reactedByMe = true, count + 1)
                    const nextReacted = !currentlyReacted;
                    const deltaCount = nextReacted ? 1 : -1;

                    return {
                      ...item,
                      content: {
                        ...item.content,
                        reactedByMe: nextReacted,
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
        const { myReaction, totalCount } = res.data;
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
                        reactedByMe: myReaction !== null,
                        reactCount: totalCount,
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
    onSettled: () => {
      // 6. Luôn làm mới ngầm các truy vấn liên quan
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
    },
  });

  return {
    toggleReact: toggleReactMutation.mutateAsync,
    isReacting: toggleReactMutation.isPending,
  };
}
