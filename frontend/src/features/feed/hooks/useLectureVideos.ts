import { useInfiniteQuery } from '@tanstack/react-query';
import { postService } from '../services/postService';

export function useLectureVideos(subjectId: number | undefined, enabled: boolean) {
  return useInfiniteQuery({
    queryKey: ['lecture-videos', subjectId],
    queryFn: async ({ pageParam }) => (await postService.getVideoPosts(subjectId, pageParam)).data,
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (page, _pages, _previous, params) => {
      const after = page.pagination?.after;
      return page.pagination?.hasNext && after != null && !params.includes(after) ? after : undefined;
    },
    enabled,
  });
}
