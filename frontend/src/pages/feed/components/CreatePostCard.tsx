import React, { useState } from 'react';
import type { SubjectItem, PostVisibility, PostType } from '@/types/post.types';
import { useAuthStore } from '@/stores/useAuthStore';
import type { UserRole } from '@/stores/useAuthStore';
import {
  Image,
  BookOpen,
  Globe,
  Users,
  Lock,
  Send,
  X,
  Video,
  MessageSquare,
  AlertCircle,
  GraduationCap,
  ShieldCheck,
  UserCheck
} from 'lucide-react';

interface CreatePostCardProps {
  subjects: SubjectItem[];
  onSubmitPost: (data: {
    content: string;
    subjectId?: number;
    visibility: PostVisibility;
    postType?: PostType;
    mediaUrls?: string[];
  }) => void;
}

export function CreatePostCard({ subjects, onSubmitPost }: CreatePostCardProps) {
  const { user, setUserRole } = useAuthStore();
  const currentRole: UserRole = user?.role || 'STUDENT';
  const isLecturerOrAdmin = currentRole === 'LECTURER' || currentRole === 'ADMIN';

  const [postType, setPostType] = useState<PostType>('SOCIAL_POST');
  const [content, setContent] = useState('');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [visibility, setVisibility] = useState<PostVisibility>('PUBLIC');
  const [attachedImages, setAttachedImages] = useState<string[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Giả lập chọn ảnh mẫu
  const handleAddSampleImage = () => {
    if (attachedImages.length >= 4) return;
    const sampleImages = [
      'https://images.unsplash.com/photo-1516321318423-f06f85e504b3?w=800&auto=format&fit=crop&q=80',
      'https://images.unsplash.com/photo-1517694712202-14dd9538aa97?w=800&auto=format&fit=crop&q=80',
    ];
    setAttachedImages([...attachedImages, sampleImages[attachedImages.length % sampleImages.length]]);
  };

  const handleRemoveImage = (index: number) => {
    setAttachedImages(attachedImages.filter((_, i) => i !== index));
  };

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMessage(null);

    // Validate: Nếu là Video bài giảng thì bắt buộc phải chọn Môn học (theo đúng PostServiceImpl.java)
    if (postType === 'LEARNING_VIDEO' && !selectedSubjectId) {
      setErrorMessage('Vui lòng chọn môn học cho video bài giảng (Bắt buộc)');
      return;
    }

    if (!content.trim() && attachedImages.length === 0) {
      setErrorMessage('Vui lòng nhập nội dung bài viết hoặc đính kèm ảnh');
      return;
    }

    setIsSubmitting(true);
    setTimeout(() => {
      onSubmitPost({
        content,
        subjectId: postType === 'LEARNING_VIDEO' ? selectedSubjectId : undefined,
        visibility: postType === 'LEARNING_VIDEO' ? 'PUBLIC' : visibility,
        postType: isLecturerOrAdmin ? postType : 'SOCIAL_POST',
        mediaUrls: attachedImages,
      });
      setContent('');
      setSelectedSubjectId(undefined);
      setAttachedImages([]);
      setPostType('SOCIAL_POST');
      setIsSubmitting(false);
    }, 400);
  };

  return (
    <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 sm:p-5 shadow-xs space-y-3 transition-colors">
      {/* Top Header: Role indicator & Role Switcher for dev testing */}
      <div className="flex items-center justify-between pb-2 border-b border-slate-100 dark:border-slate-800 text-xs">
        <div className="flex items-center gap-2">
          {currentRole === 'ADMIN' ? (
            <span className="inline-flex items-center gap-1 font-bold text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/50 border border-rose-200 dark:border-rose-900 px-2 py-0.5 rounded-full text-[10px]">
              <ShieldCheck className="w-3 h-3" />
              Quản trị viên (Admin)
            </span>
          ) : currentRole === 'LECTURER' ? (
            <span className="inline-flex items-center gap-1 font-bold text-emerald-700 dark:text-emerald-400 bg-emerald-50 dark:bg-emerald-950/50 border border-emerald-200 dark:border-emerald-900 px-2 py-0.5 rounded-full text-[10px]">
              <GraduationCap className="w-3 h-3" />
              Giảng viên (Lecturer)
            </span>
          ) : (
            <span className="inline-flex items-center gap-1 font-bold text-indigo-700 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-900 px-2 py-0.5 rounded-full text-[10px]">
              <UserCheck className="w-3 h-3" />
              Sinh viên (Student)
            </span>
          )}
        </div>

        {/* Dev Mode Role Toggle Tool */}
        <div className="flex items-center gap-1 text-[11px] text-slate-400 dark:text-slate-500">
          <span className="hidden sm:inline">Chuyển vai trò test:</span>
          {(['STUDENT', 'LECTURER', 'ADMIN'] as const).map((r) => (
            <button
              key={r}
              type="button"
              onClick={() => setUserRole(r)}
              className={`px-1.5 py-0.5 rounded text-[10px] font-bold transition-all cursor-pointer ${
                currentRole === r
                  ? 'bg-slate-800 dark:bg-slate-700 text-white shadow-xs'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-500 dark:text-slate-400 hover:bg-slate-200 dark:hover:bg-slate-700'
              }`}
            >
              {r === 'STUDENT' ? 'SV' : r === 'LECTURER' ? 'GV' : 'Admin'}
            </button>
          ))}
        </div>
      </div>

      {/* Tabs for Lecturer & Admin: Choose between Discussion or Learning Video */}
      {isLecturerOrAdmin && (
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-800/80 rounded-xl w-fit text-xs">
          <button
            type="button"
            onClick={() => {
              setPostType('SOCIAL_POST');
              setErrorMessage(null);
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
              postType === 'SOCIAL_POST'
                ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-slate-100 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <MessageSquare className="w-3.5 h-3.5 text-indigo-600 dark:text-indigo-400" />
            <span>Thảo luận học tập</span>
          </button>

          <button
            type="button"
            onClick={() => {
              setPostType('LEARNING_VIDEO');
              setVisibility('PUBLIC');
              setErrorMessage(null);
            }}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-bold transition-all cursor-pointer ${
              postType === 'LEARNING_VIDEO'
                ? 'bg-sky-600 text-white shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200'
            }`}
          >
            <Video className="w-3.5 h-3.5 text-amber-300" />
            <span>Xuất bản Video Bài Giảng</span>
            <span className="px-1.5 py-0.2 rounded-full text-[9px] bg-white/20 uppercase font-black">
              Chuyên môn
            </span>
          </button>
        </div>
      )}

      {/* Validation Alert */}
      {errorMessage && (
        <div className="flex items-center gap-2 p-2.5 bg-rose-50 dark:bg-rose-950/40 border border-rose-200 dark:border-rose-900 rounded-xl text-xs text-rose-700 dark:text-rose-300">
          <AlertCircle className="w-4 h-4 shrink-0 text-rose-600 dark:text-rose-400" />
          <span>{errorMessage}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-3">
        {/* Top: Avatar & Textarea */}
        <div className="flex gap-3">
          {/* Avatar with optional frame */}
          <div className="relative shrink-0">
            <div className="w-10 h-10 rounded-full bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-sm shadow-xs overflow-hidden">
              {user?.avatarUrl ? (
                <img src={user.avatarUrl} alt={user.username} className="w-full h-full object-cover" />
              ) : (
                user?.username?.charAt(0).toUpperCase() || 'S'
              )}
            </div>
            {user?.frameUrl && (
              <img
                src={user.frameUrl}
                alt="Avatar Frame"
                className="absolute -inset-1 w-[calc(100%+8px)] h-[calc(100%+8px)] object-contain pointer-events-none"
              />
            )}
          </div>

          <div className="flex-1">
            <textarea
              rows={postType === 'LEARNING_VIDEO' ? 3 : 2}
              value={content}
              onChange={(e) => {
                setContent(e.target.value);
                if (errorMessage) setErrorMessage(null);
              }}
              placeholder={
                postType === 'LEARNING_VIDEO'
                  ? 'Nhập tiêu đề chuyên đề, mô tả trọng tâm bài giảng và hướng dẫn ôn tập cho sinh viên...'
                  : 'Chia sẻ kinh nghiệm học tập, câu hỏi thảo luận, hoặc tài liệu ôn thi...'
              }
              className="w-full text-sm placeholder:text-slate-400 dark:placeholder:text-slate-500 text-slate-800 dark:text-slate-100 bg-transparent resize-none outline-none leading-relaxed"
            />
          </div>
        </div>

        {/* Video Upload Banner Indicator if LEARNING_VIDEO */}
        {postType === 'LEARNING_VIDEO' && (
          <div className="p-3 rounded-xl bg-sky-50/80 dark:bg-sky-950/40 border border-sky-200 dark:border-sky-800 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-2 text-xs">
            <div className="flex items-center gap-2 text-sky-900 dark:text-sky-200 font-medium">
              <Video className="w-4 h-4 text-sky-600 dark:text-sky-400" />
              <span>Video bài giảng sẽ được đính kèm vào hệ thống học tập của môn học.</span>
            </div>
            <span className="text-[11px] font-bold text-sky-700 dark:text-sky-300 bg-white dark:bg-slate-900 px-2 py-0.5 rounded border border-sky-200 dark:border-sky-800">
              Thời lượng: 42 phút • MP4 HD
            </span>
          </div>
        )}

        {/* Attached Images Preview */}
        {attachedImages.length > 0 && (
          <div className="grid grid-cols-2 sm:grid-cols-4 gap-2 pt-2">
            {attachedImages.map((url, idx) => (
              <div key={idx} className="relative rounded-xl overflow-hidden aspect-video bg-slate-100 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 group">
                <img src={url} alt={`attachment-${idx}`} className="w-full h-full object-cover" />
                <button
                  type="button"
                  onClick={() => handleRemoveImage(idx)}
                  className="absolute top-1.5 right-1.5 p-1 bg-slate-900/70 hover:bg-slate-900 text-white rounded-full transition-colors cursor-pointer"
                >
                  <X className="w-3 h-3" />
                </button>
              </div>
            ))}
          </div>
        )}

        {/* Bottom Actions Bar */}
        <div className="flex flex-wrap items-center justify-between gap-2 pt-3 border-t border-slate-100 dark:border-slate-800">
          
          <div className="flex flex-wrap items-center gap-2">
            
            {/* Subject Selector: CHỈ HIỂN THỊ KHI LÀ LEARNING_VIDEO (Bắt buộc) */}
            {postType === 'LEARNING_VIDEO' && (
              <div className="relative">
                <select
                  value={selectedSubjectId || ''}
                  onChange={(e) => {
                    setSelectedSubjectId(e.target.value ? Number(e.target.value) : undefined);
                    if (errorMessage) setErrorMessage(null);
                  }}
                  className={`text-xs font-semibold py-1.5 pl-7 pr-3 rounded-lg outline-none cursor-pointer transition-colors appearance-none border ${
                    !selectedSubjectId
                      ? 'bg-rose-50 dark:bg-rose-950/40 border-rose-300 dark:border-rose-800 text-rose-700 dark:text-rose-300 focus:border-rose-500'
                      : 'bg-slate-100 dark:bg-slate-800 hover:bg-slate-200/80 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 border-transparent focus:border-indigo-500'
                  }`}
                >
                  <option value="">Chọn môn học (Bắt buộc) *</option>
                  {subjects.map((sub) => (
                    <option key={sub.id} value={sub.id} className="dark:bg-slate-800 dark:text-slate-200">
                      {sub.code} • {sub.name}
                    </option>
                  ))}
                </select>
                <BookOpen className={`w-3.5 h-3.5 absolute left-2 top-1/2 -translate-y-1/2 pointer-events-none ${
                  !selectedSubjectId ? 'text-rose-500' : 'text-indigo-600 dark:text-indigo-400'
                }`} />
              </div>
            )}

            {/* Visibility Mode: Khi là video bài giảng thì luôn là Công khai và khoá chỉnh sửa */}
            <div className="relative">
              <select
                value={postType === 'LEARNING_VIDEO' ? 'PUBLIC' : visibility}
                disabled={postType === 'LEARNING_VIDEO'}
                onChange={(e) => setVisibility(e.target.value as PostVisibility)}
                title={
                  postType === 'LEARNING_VIDEO'
                    ? 'Video bài giảng mặc định luôn luôn công khai cho tất cả sinh viên'
                    : 'Chế độ hiển thị bài viết'
                }
                className={`text-xs font-semibold py-1.5 pl-6 pr-3 rounded-lg border outline-none transition-colors appearance-none ${
                  postType === 'LEARNING_VIDEO'
                    ? 'bg-slate-100/90 dark:bg-slate-800/80 text-slate-600 dark:text-slate-300 border-slate-200 dark:border-slate-700 cursor-not-allowed opacity-90'
                    : 'bg-slate-100 dark:bg-slate-800 hover:bg-slate-200/80 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 border-transparent focus:border-indigo-500 cursor-pointer'
                }`}
              >
                <option value="PUBLIC" className="dark:bg-slate-800">Công khai (Mọi người)</option>
                {postType !== 'LEARNING_VIDEO' && (
                  <>
                    <option value="FRIENDS" className="dark:bg-slate-800">Bạn bè</option>
                    <option value="ONLY_ME" className="dark:bg-slate-800">Chỉ mình tôi (Riêng tư)</option>
                  </>
                )}
              </select>
              {postType === 'LEARNING_VIDEO' || visibility === 'PUBLIC' ? (
                <Globe className="w-3.5 h-3.5 text-slate-500 dark:text-slate-400 absolute left-2 top-1/2 -translate-y-1/2 pointer-events-none" />
              ) : visibility === 'FRIENDS' ? (
                <Users className="w-3.5 h-3.5 text-indigo-500 dark:text-indigo-400 absolute left-2 top-1/2 -translate-y-1/2 pointer-events-none" />
              ) : (
                <Lock className="w-3.5 h-3.5 text-amber-600 dark:text-amber-400 absolute left-2 top-1/2 -translate-y-1/2 pointer-events-none" />
              )}
            </div>

            {/* Attach Image Button */}
            <button
              type="button"
              onClick={handleAddSampleImage}
              title="Đính kèm hình ảnh"
              className="p-1.5 text-slate-500 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 hover:bg-indigo-50 dark:hover:bg-indigo-950/40 rounded-lg transition-colors cursor-pointer flex items-center gap-1 text-xs font-medium"
            >
              <Image className="w-4 h-4 text-slate-600 dark:text-slate-400" />
              <span className="hidden sm:inline">Ảnh ({attachedImages.length}/4)</span>
            </button>
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            disabled={isSubmitting}
            className={`px-4 py-1.5 text-white text-xs sm:text-sm font-bold rounded-xl flex items-center gap-1.5 shadow-sm active:scale-[0.98] transition-all cursor-pointer ${
              postType === 'LEARNING_VIDEO'
                ? 'bg-sky-600 hover:bg-sky-700 shadow-sky-600/20'
                : 'bg-indigo-600 hover:bg-indigo-700 shadow-indigo-600/20'
            }`}
          >
            {isSubmitting ? (
              <span>Đang đăng...</span>
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
}

