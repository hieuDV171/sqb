import { useMutation, useQueryClient } from '@tanstack/react-query';
import { reactService, type ReactRequest } from '../services/reactService';

export function usePostReact() {
  const queryClient = useQueryClient();

  const toggleReactMutation = useMutation({
    mutationFn: (data: ReactRequest) => reactService.toggleReact(data),
    onSuccess: () => {
      // Invalidate relevant queries so fresh react counts sync
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
    },
  });

  return {
    toggleReact: toggleReactMutation.mutateAsync,
    isReacting: toggleReactMutation.isPending,
  };
}
