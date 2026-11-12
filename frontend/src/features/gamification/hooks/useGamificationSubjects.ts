import { useQuery } from '@tanstack/react-query';
import { sessionService } from '@/features/session/services/sessionService';

export function useGamificationSubjects(enabled: boolean) {
  return useQuery({
    queryKey: ['subjects', 'gamification'],
    queryFn: async () => (await sessionService.getAllSubjects()).data ?? [],
    enabled,
    staleTime: 5 * 60_000,
  });
}
