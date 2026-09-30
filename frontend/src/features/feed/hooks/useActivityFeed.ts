import { useInfiniteQuery } from '@tanstack/react-query';
import { feedService } from '../services/feedService';
import type { ActivityFeedItemDto } from '../types/feed.types';

export type FeedFilter = 'ALL' | 'POSTS' | 'SESSIONS' | 'ACHIEVEMENTS' | 'VIDEOS';

export function useActivityFeed(filter: FeedFilter = 'ALL') {
  const query = useInfiniteQuery({
    queryKey: ['activity-feeds', filter],
    queryFn: async ({ pageParam }) => {
      const response = await feedService.getActivityFeeds(pageParam as number | undefined, 20);
      return response.data;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (lastPage) => {
      if (lastPage?.pagination?.hasNext && lastPage?.pagination?.after) {
        return lastPage.pagination.after;
      }
      return undefined;
    },
    staleTime: 1000 * 30, // 30s
  });

  const allItems: ActivityFeedItemDto[] = query.data?.pages.flatMap((page) => page?.items || []) || [];

  // Filter items based on active tab
  const filteredItems = allItems.filter((item) => {
    if (filter === 'ALL') return true;
    if (filter === 'POSTS') {
      return item.actionType === 'CREATED_POST';
    }
    if (filter === 'SESSIONS') {
      return item.actionType === 'SESSION_RESOLVED_APPROVED';
    }
    if (filter === 'ACHIEVEMENTS') {
      return item.actionType === 'EARNED_BADGE' || item.actionType === 'REACHED_MILESTONE';
    }
    if (filter === 'VIDEOS') {
      return item.actionType === 'PUBLISHED_LECTURE_VIDEO';
    }
    return true;
  });

  return {
    ...query,
    items: filteredItems,
    rawItems: allItems,
  };
}
