import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { sessionService } from '@/features/session/services/sessionService';
import { toast } from '@/stores/useToastStore';
import { gamificationService } from '@/features/gamification';
import type { RebuildLeaderboardRequest } from '@/features/gamification';

export function useAdminLeaderboardSubjects() {
  return useQuery({
    queryKey: ['subjects', 'admin-leaderboard-rebuild'],
    queryFn: async () => {
      const response = await sessionService.getAllSubjects();
      return response.data ?? [];
    },
    staleTime: 5 * 60_000,
  });
}

export function useRebuildLeaderboard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (request: RebuildLeaderboardRequest) =>
      gamificationService.rebuildLeaderboard(request),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: ['gamification', 'leaderboard'] });
      toast.success(
        'Redis đã được đồng bộ lại từ lịch sử điểm trong PostgreSQL.',
        'Tái tạo bảng xếp hạng thành công'
      );
    },
  });
}
