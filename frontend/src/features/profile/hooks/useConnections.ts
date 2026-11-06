import { useInfiniteQuery } from '@tanstack/react-query';
import { userService } from '@/services/userService';
import type { CursorResponse, GlobalResponse } from '@/types/response.types';

function useConnectionList<T, Page extends CursorResponse<T>>(
  key: string,
  load: (after?: number) => Promise<GlobalResponse<Page>>,
  enabled = true,
  refreshOnFocus = false,
) {
  return useInfiniteQuery({
    queryKey: [key],
    queryFn: async ({ pageParam }) => (await load(pageParam)).data,
    initialPageParam: undefined as number | undefined,
    getNextPageParam: (last, _pages, previous) => {
      const after = last.pagination?.after;
      return last.pagination?.hasNext && after != null && after !== previous ? after : undefined;
    },
    enabled,
    ...(refreshOnFocus ? { staleTime: 0, refetchOnWindowFocus: true } : {}),
  });
}

export function useFriendLists(active: 'friends' | 'received' | 'sent') {
  return {
    friends: useConnectionList('myFriends', userService.getMyFriends, active === 'friends'),
    received: useConnectionList('receivedRequests', userService.getReceivedFriendRequests, active === 'received', true),
    sent: useConnectionList('sentRequests', userService.getSentFriendRequests, active === 'sent', true),
  };
}

export function useFollowLists(open: boolean, active: 'following' | 'followers') {
  return {
    following: useConnectionList('followingList', userService.getFollowing, open && active === 'following'),
    followers: useConnectionList('followersList', userService.getFollowers, open && active === 'followers'),
  };
}

export function useBlockedList() {
  return useConnectionList('blockedUsers', userService.getBlockedUsers);
}

export function connectionDate(date: string) {
  const value = new Date(date);
  return Number.isNaN(value.getTime()) ? undefined : value.toLocaleDateString('vi-VN');
}
