import { useQuery } from '@tanstack/react-query';
import { postService } from '../services/postService';

export const postDetailQueryKey = (postId: number) => ['post-detail', postId] as const;

export function usePostDetail(postId: number | null) {
  return useQuery({
    queryKey: postDetailQueryKey(postId ?? 0),
    queryFn: async () => {
      if (postId === null) {
        throw new Error('Mã bài viết không hợp lệ');
      }

      const response = await postService.getPostById(postId);
      return response.data;
    },
    enabled: postId !== null,
    staleTime: 1000 * 30,
  });
}
