import { useInfiniteQuery } from '@tanstack/react-query';
import { postService } from '@/features/feed/services/postService';

export function isProfilePostsBlocked(error: unknown) {
  return typeof error === 'object' && error !== null && 'code' in error && error.code === '2305';
}

export function useProfilePosts(userId: number, isOwnProfile: boolean) {
  return useInfiniteQuery({
    queryKey: ['profile-posts', userId],
    queryFn: async ({ pageParam, signal }) => {
      const response = isOwnProfile
        ? await postService.getMyPosts(pageParam, 10, signal)
        : await postService.getUserPosts(userId, pageParam, 10, signal);
      return response.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (last, _pages, previous) => {
      const after = last.pagination.after;
      return last.pagination.hasNext && after != null && after !== previous ? after : undefined;
    },
    refetchOnMount: 'always',
    refetchOnWindowFocus: 'always',
    retry: (count, error) => !isProfilePostsBlocked(error) && count < 1,
  });
}
