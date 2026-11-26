import { useEffect, useEffectEvent, useMemo, useState } from 'react';
import { replaceEqualDeep, useInfiniteQuery, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { getConnectionSnapshot, subscribeToConnection } from '@/api/connectivity';
import { getAuthEventMarker, useAuthStore } from '@/stores/useAuthStore';
import { feedService } from '../services/feedService';
import { readFeedSnapshot, saveFeedSnapshot, type ActivityFeedPage } from '../services/feedSnapshot';
import type { ActivityFeedItemDto } from '../types/feed.types';
import { useNewFeedCount } from './useNewFeedCount';

export type FeedFilter = 'ALL' | 'POSTS' | 'SESSIONS' | 'ACHIEVEMENTS' | 'VIDEOS';
type FeedData = InfiniteData<ActivityFeedPage, number | undefined>;
type FeedRefresh = { page: ActivityFeedPage; requestedAt: number };
type PendingRefresh = FeedRefresh & { previous: ActivityFeedPage };

const firstPageData = (page: ActivityFeedPage): FeedData => ({ pages: [page], pageParams: [undefined] });

function hasSamePagination(previous: ActivityFeedPage, page: ActivityFeedPage) {
  return previous.pagination?.after === page.pagination?.after &&
    previous.pagination?.hasNext === page.pagination?.hasNext &&
    previous.items.length === page.items.length &&
    previous.items.every((item, index) => item.feedId === page.items[index].feedId);
}

export function useActivityFeed(filter: FeedFilter = 'ALL') {
  const client = useQueryClient();
  const userId = useAuthStore((state) => state.user?.id);
  const role = useAuthStore((state) => state.user?.role);
  const [sessionMarker] = useState(getAuthEventMarker);
  const queryKey = useMemo(() => ['activity-feeds', userId, role] as const, [role, userId]);
  const [isRestored, setRestored] = useState(() => Boolean(client.getQueryData(queryKey)));
  const [pending, setPending] = useState<PendingRefresh>();
  const enabled = Boolean(userId) && filter !== 'VIDEOS' && isRestored;
  const { newCount, markRefreshed } = useNewFeedCount(enabled);

  function isCurrentSession() {
    const session = useAuthStore.getState();
    return session.user?.id === userId && session.user?.role === role && session.tokenType === 'ACCESS' &&
      session.accountState === 'ACTIVE' && getAuthEventMarker() === sessionMarker;
  }

  const readSnapshot = useEffectEvent((isMounted: () => boolean) => {
    if (!userId) return;
    void readFeedSnapshot({ userId, role, sessionMarker }, isCurrentSession).then((snapshot) => {
      if (!isMounted() || !isCurrentSession()) return;
      if (snapshot && !client.getQueryData(queryKey)) {
        client.setQueryData(queryKey, firstPageData(snapshot.page), { updatedAt: snapshot.requestedAt });
      }
      setRestored(true);
    });
  });

  useEffect(() => {
    if (isRestored) return;
    let mounted = true;
    readSnapshot(() => mounted);
    return () => { mounted = false; };
  }, [isRestored, userId]);

  async function fetchPage(after: number | undefined, signal: AbortSignal) {
    const requestedAt = Date.now();
    const { data: page } = await feedService.getActivityFeeds(after, 20, signal);
    return { page, requestedAt };
  }

  function persistFirstPage(result: FeedRefresh, signal?: AbortSignal) {
    if (!userId) return;
    void saveFeedSnapshot({ userId, role, sessionMarker, ...result }, () => !signal?.aborted && isCurrentSession());
  }

  const query = useInfiniteQuery({
    queryKey,
    queryFn: async ({ pageParam, signal }) => {
      const result = await fetchPage(pageParam, signal);
      if (pageParam === undefined) persistFirstPage(result, signal);
      return result.page;
    },
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (page, _pages, _previous, params) => {
      const after = page.pagination?.after;
      return page.pagination?.hasNext && after != null && !params.includes(after) ? after : undefined;
    },
    enabled,
    staleTime: Infinity,
    refetchOnMount: false,
    refetchOnReconnect: false,
  });

  const visibleFirstPage = query.data?.pages[0];
  const availableRefresh = pending?.previous === visibleFirstPage ? pending : undefined;
  const pendingCount = availableRefresh?.page.items.filter((item) =>
    item.feedId > (availableRefresh.previous.items[0]?.feedId ?? 0)).length ?? 0;

  async function refreshFeed(manual: boolean, isActive = () => true): Promise<boolean> {
    if (!isCurrentSession()) return false;
    const initialState = client.getQueryState<FeedData>(queryKey);
    const previous = initialState?.data?.pages[0];
    const staged = manual && pending?.previous === previous ? pending : undefined;
    if (!staged && navigator.onLine === false) return false;
    if (!previous) {
      const startedAt = Date.now();
      const result = await query.refetch({ cancelRefetch: false });
      if (result.isSuccess) markRefreshed(startedAt);
      return result.isSuccess;
    }
    if (initialState.isInvalidated && initialState.fetchStatus !== 'idle') return true;

    try {
      const incoming = staged ?? await client.fetchQuery<FeedRefresh>({
        queryKey: ['activity-feed-refresh', userId, role],
        queryFn: ({ signal }) => fetchPage(undefined, signal),
        staleTime: 0,
        gcTime: 0,
      });
      if (!isActive() || !isCurrentSession()) return false;
      const state = client.getQueryState<FeedData>(queryKey);
      if (state?.data?.pages[0] !== previous ||
          (state.isInvalidated && (!initialState.isInvalidated || state.fetchStatus !== 'idle'))) return true;
      const changed = replaceEqualDeep(previous, incoming.page) !== previous;
      if (!staged) persistFirstPage(incoming);

      if (changed && !manual && window.scrollY > 1) {
        setPending({ ...incoming, previous });
        return true;
      }

      if (changed || state.isInvalidated || state.status === 'error') {
        // Prevent a pending older-page request from restoring the previous first page.
        void client.cancelQueries({ queryKey, exact: true });
        client.setQueryData<FeedData>(queryKey, (latest) => {
          if (latest && hasSamePagination(previous, incoming.page)) {
            return { ...latest, pages: [incoming.page, ...latest.pages.slice(1)] };
          }
          return firstPageData(incoming.page);
        });
      }
      setPending(undefined);
      markRefreshed(incoming.requestedAt);
      return true;
    } catch {
      return false;
    }
  }

  const refreshOnConnection = useEffectEvent((reconnected: boolean, isActive: () => boolean) => {
    if (client.getQueryData(queryKey) || (reconnected && query.isLoadingError)) {
      void refreshFeed(false, isActive);
    }
  });

  useEffect(() => {
    if (!enabled) return;
    let active = true;
    const isActive = () => active;
    const initialConnection = getConnectionSnapshot();
    let disconnected = initialConnection.status !== 'connected' || initialConnection.checking;
    // Wait for the infinite-query observer to finish enabling on initial hydration.
    if (!disconnected) queueMicrotask(() => {
      if (active && !disconnected) refreshOnConnection(false, isActive);
    });
    const unsubscribe = subscribeToConnection(() => {
      const { status, checking } = getConnectionSnapshot();
      if (status !== 'connected') disconnected = true;
      else if (disconnected && !checking) {
        disconnected = false;
        refreshOnConnection(true, isActive);
      }
    });
    return () => { active = false; unsubscribe(); };
  }, [enabled, userId]);

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
    isLoading: !isRestored || query.isLoading,
    newCount: Math.max(newCount, pendingCount),
    updateAvailable: Boolean(availableRefresh),
    refresh: () => refreshFeed(true),
    items: filteredItems,
    rawItems: allItems,
  };
}
