import { useInfiniteQuery } from '@tanstack/react-query';
import { practiceService } from '../services/practiceService';
import type { UserSubmissionsResponse, GetUserSessionsParams } from '../types/practice.types';

export function useUserProposedSessions(
  userId: number | null | undefined,
  filters?: Omit<GetUserSessionsParams, 'after'>
) {
  return useInfiniteQuery<UserSubmissionsResponse>({
    queryKey: ['userProposedSessions', userId, filters?.subject_id, filters?.status],
    queryFn: async ({ pageParam }) => {
      if (!userId) throw new Error('User ID is required');
      const res = await practiceService.getUserProposedSessions(userId, {
        after: pageParam as number | undefined,
        limit: filters?.limit || 10,
        subject_id: filters?.subject_id,
        status: filters?.status,
      });
      if (!res.data) throw new Error(res.message || 'Không thể tải danh sách phiên của người dùng');
      return res.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage.pagination?.hasNext && lastPage.pagination.after) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    enabled: !!userId && !isNaN(userId) && userId > 0,
    staleTime: 1000 * 60 * 3,
  });
}
