import { useMutation, useQueryClient } from '@tanstack/react-query';
import { postService } from '../services/postService';
import type { CreatePostRequest, UpdatePostRequest, UpdateLecturerNoteRequest } from '@/types/post.types';
import { useToastStore } from '@/stores/useToastStore';

export function usePostMutations() {
  const queryClient = useQueryClient();
  const { addToast } = useToastStore();

  const createPostMutation = useMutation({
    mutationFn: (data: CreatePostRequest) => postService.createPost(data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
      queryClient.invalidateQueries({ queryKey: ['lecture-videos'] });
      queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      addToast({
        type: 'success',
        title: 'Thành công',
        message: 'Đã xuất bản bài viết lên bảng tin!',
      });
    },
  });

  const updatePostMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdatePostRequest }) =>
      postService.updatePost(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
      queryClient.invalidateQueries({ queryKey: ['lecture-videos'] });
      queryClient.invalidateQueries({ queryKey: ['post-detail', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      addToast({
        type: 'success',
        title: 'Thành công',
        message: 'Bài viết đã được chỉnh sửa!',
      });
    },
  });

  const deletePostMutation = useMutation({
    mutationFn: (id: number) => postService.deletePost(id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
      queryClient.invalidateQueries({ queryKey: ['lecture-videos'] });
      queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      addToast({
        type: 'success',
        title: 'Đã xóa',
        message: 'Bài viết đã được gỡ bỏ.',
      });
    },
  });

  const updateLecturerNoteMutation = useMutation({
    mutationFn: ({ id, data }: { id: number; data: UpdateLecturerNoteRequest }) =>
      postService.updateLecturerNote(id, data),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: ['activity-feeds'] });
      queryClient.invalidateQueries({ queryKey: ['post-detail', variables.id] });
      queryClient.invalidateQueries({ queryKey: ['profile-posts'] });
      addToast({
        type: 'success',
        title: 'Thành công',
        message: 'Đã lưu nhận xét chuyên môn của Giảng viên!',
      });
    },
  });

  return {
    createPost: createPostMutation.mutateAsync,
    isCreating: createPostMutation.isPending,
    updatePost: updatePostMutation.mutateAsync,
    isUpdating: updatePostMutation.isPending,
    deletePost: deletePostMutation.mutateAsync,
    isDeleting: deletePostMutation.isPending,
    updateLecturerNote: updateLecturerNoteMutation.mutateAsync,
    isUpdatingNote: updateLecturerNoteMutation.isPending,
  };
}
