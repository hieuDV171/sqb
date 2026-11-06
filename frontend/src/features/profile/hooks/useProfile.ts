import { useQuery } from '@tanstack/react-query';
import { userService } from '@/services/userService';

export function useProfile(target: 'me' | number, enabled: boolean) {
  return useQuery({
    queryKey: ['userProfile', target],
    queryFn: async () => {
      const response = target === 'me'
        ? await userService.getMyProfile()
        : await userService.getUserProfile(target);
      return response.data;
    },
    enabled,
    ...(target !== 'me' ? { refetchOnMount: 'always', refetchOnWindowFocus: 'always' } as const : {}),
  });
}
