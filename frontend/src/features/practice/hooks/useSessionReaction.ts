import { useIsMutating, useMutation, useQueryClient, type InfiniteData } from '@tanstack/react-query';
import { reactService, type ReactionType } from '@/features/feed/services/reactService';
import type { PracticeCatalogResponse, SessionQuestionsResponse } from '../types/practice.types';
import { practiceCatalogQueryKey } from './usePracticeCatalog';

export interface SessionReactionState {
  reactCount: number;
  myReaction: ReactionType | null;
}

const sessionListKeys = [practiceCatalogQueryKey, ['my-submissions']] as const;

export function useSessionReaction(sessionId: number) {
  const queryClient = useQueryClient();
  const mutationKey = ['session-reaction', sessionId] as const;
  const practiceQuery = { queryKey: ['practiceSession', sessionId], exact: true };
  const isUpdating = useIsMutating({ mutationKey, exact: true }) > 0;

  const updateReaction = (reaction: SessionReactionState) => {
    queryClient.setQueryData<SessionQuestionsResponse>(practiceQuery.queryKey, (current) =>
      current ? { ...current, ...reaction } : current,
    );
    for (const queryKey of sessionListKeys) {
      queryClient.setQueriesData<InfiniteData<PracticeCatalogResponse>>({ queryKey }, (current) => current && ({
        ...current,
        pages: current.pages.map((page) => ({
          ...page,
          contents: page.contents.map((session) => session.sessionId === sessionId ? { ...session, ...reaction } : session),
        })),
      }));
    }
  };

  const mutation = useMutation({
    mutationKey,
    mutationFn: async ({ myReaction }: SessionReactionState) => {
      const { data } = await reactService.toggleReact(
        { targetType: 'SESSION', targetId: sessionId, reactionType: myReaction ?? 'LIKE' },
      );
      if (!data) throw new Error('Không thể cập nhật cảm xúc.');
      return data;
    },
    onMutate: async ({ reactCount, myReaction }) => {
      await Promise.all([
        queryClient.cancelQueries(practiceQuery),
        ...sessionListKeys.map((queryKey) => queryClient.cancelQueries({ queryKey })),
      ]);
      updateReaction({
        reactCount: Math.max(0, reactCount + (myReaction ? -1 : 1)),
        myReaction: myReaction ? null : 'LIKE',
      });
    },
    onSuccess: ({ totalCount, myReaction }) => updateReaction({ reactCount: totalCount, myReaction }),
    onError: (_error, previous) => {
      updateReaction(previous);
    },
    onSettled: () => {
      void queryClient.invalidateQueries(practiceQuery);
      for (const queryKey of sessionListKeys) void queryClient.invalidateQueries({ queryKey });
    },
  });

  const toggleReaction = (current: SessionReactionState) => {
    if (!queryClient.isMutating({ mutationKey, exact: true })) mutation.mutate(current);
  };

  return { isUpdating, toggleReaction, error: mutation.error };
}
