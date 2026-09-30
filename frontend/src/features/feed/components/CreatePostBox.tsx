import React, { useState, useRef, useEffect } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { usePostMutations } from '../hooks/usePostMutations';
import { sessionService } from '@/features/session/services/sessionService';
import { mediaService } from '@/services/mediaService';
import { toast } from '@/stores/useToastStore';
import type { SubjectResponse } from '@/features/session/types/session.types';
import type { PostVisibility, PostType, MediaItem } from '@/types/post.types';
import {
  Image as ImageIcon,
  Video,
  Globe,
  Send,
  X,
  Loader2,
  GraduationCap,
  FileText,
} from 'lucide-react';
import { cn } from '@/lib/utils';
import { axiosClient } from '@/api/axiosClient';
import { PostVisibilityDropdown } from './PostVisibilityDropdown';
import { SubjectSelectDropdown } from './SubjectSelectDropdown';

export const CreatePostBox: React.FC = () => {
  const { user } = useAuthStore();
  const { createPost, isCreating } = usePostMutations();

  const isLecturerOrAdmin = user?.role === 'LECTURER' || user?.role === 'ADMIN';

  const [content, setContent] = useState('');
  const [visibility, setVisibility] = useState<PostVisibility>('PUBLIC');
  const [postType, setPostType] = useState<PostType>('SOCIAL_POST');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [subjects, setSubjects] = useState<SubjectResponse[]>([]);
  const [mediaList, setMediaList] = useState<MediaItem[]>([]);
  const [isUploadingMedia, setIsUploadingMedia] = useState(false);

  const fileInputRef = useRef<HTMLInputElement>(null);
  const videoInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    // Chỉ tải danh sách môn học khi là Giảng viên / Admin
    if (isLecturerOrAdmin) {
      const loadSubjects = async () => {
        try {
          // 1. Thử lấy danh sách môn học đang tham gia
          const res = await sessionService.getMyEnrolledSubjects();
          if (res?.data && res.data.length > 0) {
            setSubjects(res.data);
            return;
          }

          // 2. Nếu là Giảng viên, lấy từ danh sách lớp học phần giảng dạy
          if (user?.role === 'LECTURER') {
            const classRes = await axiosClient.get<any>('/lecturer/course-classes/my-classes');
            if (classRes?.data?.data && Array.isArray(classRes.data.data)) {
              const uniqueMap = new Map<number, SubjectResponse>();
              classRes.data.data.forEach((c: any) => {
                if (c.subjectId && !uniqueMap.has(c.subjectId)) {
                  uniqueMap.set(c.subjectId, {
                    subjectId: c.subjectId,
                    code: c.subjectCode || '',
                    name: c.subjectName || '',
                  });
                }
              });
              if (uniqueMap.size > 0) {
                setSubjects(Array.from(uniqueMap.values()));
                return;
              }
            }
          }

          // 3. Nếu là Admin, lấy tất cả môn học trong hệ thống
          if (user?.role === 'ADMIN') {
            const adminRes = await axiosClient.get<any>('/admin/subjects');
            if (adminRes?.data?.data && Array.isArray(adminRes.data.data)) {
              setSubjects(adminRes.data.data);
            }
          }
        } catch (err) {
          console.warn('Không thể tải danh sách môn học:', err);
        }
      };

      loadSubjects();
    }
  }, [isLecturerOrAdmin, user?.role]);

  // Sinh viên không bao giờ được chọn LEARNING_VIDEO
  useEffect(() => {
    if (!isLecturerOrAdmin && postType !== 'SOCIAL_POST') {
      setPostType('SOCIAL_POST');
    }
  }, [isLecturerOrAdmin, postType]);

  // Video bài giảng luôn luôn có visibility là PUBLIC
  useEffect(() => {
    if (postType === 'LEARNING_VIDEO') {
      setVisibility('PUBLIC');
    }
  }, [postType]);

  const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>, forceVideo = false) => {
    const files = e.target.files;
    if (!files || files.length === 0) return;

    setIsUploadingMedia(true);
    try {
      const uploadPromises = Array.from(files).map(async (file) => {
        const { objectKey, publicUrl } = await mediaService.uploadViaPresign(file, 'POST');
        const isVideo = forceVideo || file.type.startsWith('video/');
        const mediaItem: MediaItem = {
          objectKey,
          url: publicUrl,
          mediaType: isVideo ? 'VIDEO' : 'IMAGE',
          size: file.size,
          name: file.name,
        };
        return mediaItem;
      });

      const uploaded = await Promise.all(uploadPromises);
      setMediaList((prev) => [...prev, ...uploaded]);
    } catch (err: any) {
      toast.error(err?.message || 'Lỗi khi tải tệp lên');
    } finally {
      setIsUploadingMedia(false);
      if (e.target) {
        e.target.value = '';
      }
    }
  };

  const removeMedia = (index: number) => {
    setMediaList((prev) => prev.filter((_, i) => i !== index));
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // 1. Kiểm tra validation cho LEARNING_VIDEO
    if (postType === 'LEARNING_VIDEO') {
      if (!isLecturerOrAdmin) {
        toast.error('Chỉ Giảng viên hoặc Quản trị viên mới được đăng video bài giảng');
        return;
      }
      if (!selectedSubjectId) {
        toast.error('Video bài giảng bắt buộc phải gắn với 1 môn học');
        return;
      }
      const hasVideo = mediaList.some((m) => m.mediaType === 'VIDEO');
      if (!hasVideo) {
        toast.error('Vui lòng tải lên tệp video bài giảng');
        return;
      }
      if (!content.trim()) {
        toast.error('Vui lòng nhập tiêu đề hoặc tóm tắt nội dung bài giảng');
        return;
      }
    } else {
      // 2. SOCIAL_POST
      if (!content.trim() && mediaList.length === 0) {
        toast.error('Vui lòng nhập nội dung bài viết hoặc đính kèm ảnh/video');
        return;
      }
    }

    try {
      await createPost({
        content: content.trim(),
        visibility: postType === 'LEARNING_VIDEO' ? 'PUBLIC' : visibility,
        postType: isLecturerOrAdmin ? postType : 'SOCIAL_POST',
        subjectId: postType === 'LEARNING_VIDEO' ? selectedSubjectId : undefined,
        mediaUrls: mediaList,
      });

      setContent('');
      setMediaList([]);
      setSelectedSubjectId(undefined);
      if (postType === 'LEARNING_VIDEO') {
        setPostType('SOCIAL_POST');
      }
    } catch {
      // Toast handled by mutation hook
    }
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 shadow-xs mb-6 relative z-30 transition-all duration-300">
      {/* Role & Post Type Switcher (Dành riêng cho Giảng viên / Admin) */}
      {isLecturerOrAdmin && (
        <div className="bg-slate-50/90 dark:bg-slate-800/50 px-4 py-2.5 border-b border-slate-200/70 dark:border-slate-800 flex items-center justify-between flex-wrap gap-2 text-xs rounded-t-3xl">
          <div className="flex items-center gap-1.5 p-1 bg-slate-200/60 dark:bg-slate-700/60 rounded-2xl">
            <button
              type="button"
              onClick={() => {
                setPostType('SOCIAL_POST');
              }}
              className={cn(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold transition-all cursor-pointer',
                postType === 'SOCIAL_POST'
                  ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
              )}
            >
              <FileText className="w-3.5 h-3.5 text-blue-500" />
              <span>Bài viết thông thường</span>
            </button>

            <button
              type="button"
              onClick={() => {
                setPostType('LEARNING_VIDEO');
                setVisibility('PUBLIC');
              }}
              className={cn(
                'flex items-center gap-1.5 px-3 py-1.5 rounded-xl font-semibold transition-all cursor-pointer',
                postType === 'LEARNING_VIDEO'
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400'
              )}
            >
              <GraduationCap className="w-3.5 h-3.5 text-amber-300" />
              <span>Video bài giảng môn học</span>
            </button>
          </div>

          <span className="text-[11px] font-medium text-slate-500 dark:text-slate-400">
            {postType === 'LEARNING_VIDEO'
              ? '🎓 Chế độ đăng bài giảng cho sinh viên'
              : '📝 Chế độ bài viết mạng xã hội'}
          </span>
        </div>
      )}

      {/* LEARNING_VIDEO Banner for Lecturer */}
      {postType === 'LEARNING_VIDEO' && (
        <div className="bg-gradient-to-r from-indigo-500/10 via-purple-500/10 to-transparent border-b border-indigo-200/60 dark:border-indigo-900/40 px-4 py-2.5 flex items-center justify-between text-xs">
          <div className="flex items-center gap-2 text-indigo-700 dark:text-indigo-300 font-medium">
            <GraduationCap className="w-4 h-4 text-indigo-600 dark:text-indigo-400 shrink-0" />
            <span>
              Video bài giảng yêu cầu <strong>chọn môn học</strong> và luôn ở chế độ <strong>Công khai (PUBLIC)</strong>.
            </span>
          </div>
          <button
            type="button"
            onClick={() => setPostType('SOCIAL_POST')}
            className="text-xs text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
          >
            Quay lại bài viết thường
          </button>
        </div>
      )}

      <form onSubmit={handleSubmit} className="p-4 md:p-5">
        <div className="flex gap-3.5 items-start">
          <div className="relative shrink-0">
            <img
              src={
                user?.avatarUrl ||
                'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150'
              }
              alt={user?.username || 'Avatar'}
              className="w-10 h-10 md:w-11 md:h-11 rounded-full object-cover ring-2 ring-slate-100 dark:ring-slate-800"
            />
            {user?.role && (
              <span
                className={`absolute -bottom-1 -right-1 text-[9px] font-bold px-1.5 py-0.2 rounded-full border border-white dark:border-slate-900 ${
                  user.role === 'LECTURER'
                    ? 'bg-amber-500 text-white'
                    : user.role === 'ADMIN'
                    ? 'bg-rose-500 text-white'
                    : 'bg-blue-500 text-white'
                }`}
              >
                {user.role === 'LECTURER' ? 'GV' : user.role === 'ADMIN' ? 'AD' : 'SV'}
              </span>
            )}
          </div>

          <div className="flex-1">
            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder={
                postType === 'LEARNING_VIDEO'
                  ? 'Nhập tiêu đề hoặc tóm tắt nội dung video bài giảng hôm nay... *'
                  : 'Bạn có câu hỏi chuyên môn, hoặc chia sẻ tài liệu học tập gì hôm nay?...'
              }
              rows={3}
              className="w-full bg-slate-50 dark:bg-slate-800/60 rounded-2xl p-3 text-sm md:text-base text-slate-800 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden focus:ring-2 focus:ring-blue-500/30 transition-all border border-transparent focus:border-blue-500/40 resize-none"
            />

            {/* Media Upload Previews */}
            {mediaList.length > 0 && (
              <div className="flex flex-wrap gap-2.5 mt-3">
                {mediaList.map((media, idx) => (
                  <div
                    key={idx}
                    className="relative rounded-2xl overflow-hidden border border-slate-200 dark:border-slate-700 bg-slate-100 dark:bg-slate-800 w-28 h-28 group shadow-xs"
                  >
                    {media.mediaType === 'VIDEO' ? (
                      <div className="w-full h-full relative flex items-center justify-center bg-slate-900">
                        <video
                          src={media.url}
                          className="w-full h-full object-cover opacity-80"
                          controls={false}
                        />
                        <div className="absolute inset-0 flex items-center justify-center">
                          <span className="p-2 rounded-full bg-black/60 text-white">
                            <Video className="w-5 h-5 text-indigo-400" />
                          </span>
                        </div>
                        <span className="absolute bottom-1 left-1.5 text-[10px] font-bold text-white bg-indigo-600/90 px-1.5 py-0.2 rounded-md">
                          Video
                        </span>
                      </div>
                    ) : (
                      <img
                        src={media.url}
                        alt="attachment"
                        className="w-full h-full object-cover"
                      />
                    )}
                    <button
                      type="button"
                      onClick={() => removeMedia(idx)}
                      className="absolute top-1.5 right-1.5 p-1 bg-black/60 hover:bg-black text-white rounded-full transition-colors cursor-pointer shadow-xs"
                    >
                      <X className="w-3.5 h-3.5" />
                    </button>
                  </div>
                ))}
              </div>
            )}

            {isUploadingMedia && (
              <div className="flex items-center gap-2 mt-2 text-xs text-blue-600 dark:text-blue-400 font-medium">
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>Đang tải tệp tin lên kho lưu trữ MinIO S3...</span>
              </div>
            )}

            {/* Subject Selector: CHỈ HIỂN THỊ KHI LÀ LEARNING_VIDEO (BẮT BUỘC) */}
            {postType === 'LEARNING_VIDEO' && (
              <div className="flex flex-col sm:flex-row sm:items-center gap-2 mt-3 pt-3 border-t border-slate-100 dark:border-slate-800/80">
                <span className="text-xs font-semibold text-indigo-900 dark:text-indigo-200 shrink-0">
                  Môn học bài giảng <span className="text-rose-500">*</span>:
                </span>
                <SubjectSelectDropdown
                  subjects={subjects}
                  value={selectedSubjectId}
                  onChange={setSelectedSubjectId}
                  disabled={isCreating}
                />
              </div>
            )}
          </div>
        </div>

        {/* Footer Actions */}
        <div className="flex items-center justify-between mt-3 pt-2.5 border-t border-slate-100 dark:border-slate-800/80 flex-wrap gap-2">
          <div className="flex items-center gap-2 flex-wrap">
            {/* Input file cho ảnh & video */}
            <input
              type="file"
              ref={fileInputRef}
              onChange={(e) => handleFileSelect(e, false)}
              multiple
              accept={postType === 'LEARNING_VIDEO' ? 'video/*' : 'image/*,video/*'}
              className="hidden"
            />
            {/* Input riêng cho video bài giảng */}
            <input
              type="file"
              ref={videoInputRef}
              onChange={(e) => handleFileSelect(e, true)}
              accept="video/*"
              className="hidden"
            />

            {postType === 'LEARNING_VIDEO' ? (
              <button
                type="button"
                onClick={() => videoInputRef.current?.click()}
                disabled={isUploadingMedia || isCreating}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs md:text-sm font-semibold bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 hover:bg-indigo-100 transition-colors cursor-pointer"
              >
                <Video className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
                <span>Tải tệp video bài giảng</span>
              </button>
            ) : (
              <button
                type="button"
                onClick={() => fileInputRef.current?.click()}
                disabled={isUploadingMedia || isCreating}
                className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs md:text-sm font-medium text-slate-600 dark:text-slate-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-slate-800 transition-colors cursor-pointer"
              >
                <ImageIcon className="w-4 h-4 text-emerald-500" />
                <span>Ảnh / Video</span>
              </button>
            )}

            {/* Visibility Selector: Luôn luôn hiển thị ngay từ đầu cho mọi màn hình */}
            {postType === 'LEARNING_VIDEO' ? (
              <div className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-emerald-50 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 font-semibold border border-emerald-200/60 dark:border-emerald-800/60 text-xs">
                <Globe className="w-3.5 h-3.5 text-emerald-600 dark:text-emerald-400 shrink-0" />
                <span>Công khai (PUBLIC)</span>
              </div>
            ) : (
              <PostVisibilityDropdown
                value={visibility}
                onChange={setVisibility}
                disabled={isCreating}
              />
            )}
          </div>

          <button
            type="submit"
            disabled={
              isCreating ||
              isUploadingMedia ||
              (postType === 'LEARNING_VIDEO'
                ? !selectedSubjectId || !mediaList.some((m) => m.mediaType === 'VIDEO') || !content.trim()
                : !content.trim() && mediaList.length === 0)
            }
            className={cn(
              'flex items-center gap-2 px-5 py-2 rounded-2xl text-white font-medium text-sm shadow-md active:scale-95 transition-all duration-200 cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed',
              postType === 'LEARNING_VIDEO'
                ? 'bg-indigo-600 hover:bg-indigo-700 shadow-indigo-500/20'
                : 'bg-blue-600 hover:bg-blue-700 shadow-blue-500/20'
            )}
          >
            {isCreating ? (
              <>
                <Loader2 className="w-4 h-4 animate-spin" />
                <span>{postType === 'LEARNING_VIDEO' ? 'Đang xuất bản...' : 'Đang đăng...'}</span>
              </>
            ) : (
              <>
                <span>{postType === 'LEARNING_VIDEO' ? 'Xuất bản bài giảng' : 'Đăng bài'}</span>
                <Send className="w-3.5 h-3.5" />
              </>
            )}
          </button>
        </div>
      </form>
    </div>
  );
};
