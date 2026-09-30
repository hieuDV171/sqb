import { axiosClient } from '@/api/axiosClient';
import type { GlobalResponse } from '@/types/response.types';
import type { CursorResponse } from '@/types/user.types';
import type {
  PostResponseDto,
  CreatePostRequest,
  UpdatePostRequest,
  UpdatePostResponseDto,
  DeletePostResponseDto,
  VideoPostItemDto,
  UpdateLecturerNoteRequest,
} from '@/types/post.types';

export const postService = {
  /**
   * Tạo bài viết mới
   * POST /api/v1/posts
   */
  createPost: async (data: CreatePostRequest): Promise<GlobalResponse<PostResponseDto>> => {
    return (await axiosClient.post<GlobalResponse<PostResponseDto>>('/posts', data)) as any;
  },

  /**
   * Lấy danh sách bài viết của tôi (Cursor Pagination)
   * GET /api/v1/posts/me
   */
  getMyPosts: async (
    after?: number,
    limit: number = 10
  ): Promise<GlobalResponse<CursorResponse<PostResponseDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<PostResponseDto>>>('/posts/me', {
      params: { after, limit },
    })) as any;
  },

  /**
   * Lấy danh sách bài viết của người dùng khác
   * GET /api/v1/users/{userId}/posts
   */
  getUserPosts: async (
    userId: number,
    after?: number,
    limit: number = 10
  ): Promise<GlobalResponse<CursorResponse<PostResponseDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<PostResponseDto>>>(
      `/users/${userId}/posts`,
      {
        params: { after, limit },
      }
    )) as any;
  },

  /**
   * Lấy chi tiết một bài viết theo ID
   * GET /api/v1/posts/{id}
   */
  getPostById: async (id: number): Promise<GlobalResponse<PostResponseDto>> => {
    return (await axiosClient.get<GlobalResponse<PostResponseDto>>(`/posts/${id}`)) as any;
  },

  /**
   * Chỉnh sửa bài viết
   * PUT /api/v1/posts/{id}
   */
  updatePost: async (
    id: number,
    data: UpdatePostRequest
  ): Promise<GlobalResponse<UpdatePostResponseDto>> => {
    return (await axiosClient.put<GlobalResponse<UpdatePostResponseDto>>(`/posts/${id}`, data)) as any;
  },

  /**
   * Giảng viên / Admin thêm hoặc sửa ghi chú chuyên môn cho bài viết
   * PUT /api/v1/posts/{id}/lecturer-notes
   */
  updateLecturerNote: async (
    id: number,
    data: UpdateLecturerNoteRequest
  ): Promise<GlobalResponse<void>> => {
    return (await axiosClient.put<GlobalResponse<void>>(`/posts/${id}/lecturer-notes`, data)) as any;
  },

  /**
   * Xóa bài viết
   * DELETE /api/v1/posts/{id}
   */
  deletePost: async (id: number): Promise<GlobalResponse<DeletePostResponseDto>> => {
    return (await axiosClient.delete<GlobalResponse<DeletePostResponseDto>>(`/posts/${id}`)) as any;
  },

  /**
   * Lấy danh sách video bài giảng môn học (Learning Videos Feed)
   * GET /api/v1/video-posts
   */
  getVideoPosts: async (
    subjectId?: number,
    after?: number,
    limit: number = 2
  ): Promise<GlobalResponse<CursorResponse<VideoPostItemDto>>> => {
    return (await axiosClient.get<GlobalResponse<CursorResponse<VideoPostItemDto>>>('/video-posts', {
      params: { subject_id: subjectId, after, limit },
    })) as any;
  },
};
