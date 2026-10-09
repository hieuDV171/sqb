import React, { useState } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { usePostReact } from '../hooks/usePostReact';
import { usePostMutations } from '../hooks/usePostMutations';
import { CommentSection } from './CommentSection';
import { LecturerNoteModal } from './LecturerNoteModal';
import { ReportModal } from '@/features/report';
import type { ActivityFeedItemDto } from '../types/feed.types';
import { useToastStore } from '@/stores/useToastStore';
import {
  Heart,
  MessageCircle,
  Share2,
  MoreHorizontal,
  Award,
  Globe,
  Users,
  Lock,
  Tag,
  Trash2,
  ShieldAlert,
} from 'lucide-react';

interface PostFeedCardProps {
  item: ActivityFeedItemDto;
}

export const PostFeedCard: React.FC<PostFeedCardProps> = ({ item }) => {
  const { user } = useAuthStore();
  const { toggleReact } = usePostReact();
  const { deletePost } = usePostMutations();
  const { addToast } = useToastStore();

  const { actor, content, createdAt, targetId } = item;

  const isLiked = content.reactedByMe || false;
  const likeCount = content.reactCount || 0;
  const [showComments, setShowComments] = useState<boolean>(false);
  const [showMenu, setShowMenu] = useState<boolean>(false);
  const [isLecturerModalOpen, setIsLecturerModalOpen] = useState<boolean>(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState<boolean>(false);
  const [isContentExpanded, setIsContentExpanded] = useState<boolean>(false);

  const isAuthor = user?.id === actor.userId;
  const isLecturerOrAdmin = user?.role === 'LECTURER' || user?.role === 'ADMIN';

  const handleLike = () => {
    toggleReact({
      targetType: (item.targetType as any) || 'POST',
      targetId,
      reactionType: 'LIKE',
    });
  };

  const handleShare = () => {
    navigator.clipboard.writeText(`${window.location.origin}/feed?postId=${targetId}`);
    addToast({
      type: 'info',
      title: 'Đã sao chép liên kết',
      message: 'Đường dẫn bài viết đã được lưu vào bộ nhớ tạm.',
    });
  };

  const handleDelete = async () => {
    if (window.confirm('Bạn có chắc chắn muốn xóa bài viết này không?')) {
      await deletePost(targetId);
    }
  };

  const textContent = content.description || content.title || '';
  const isLongText = textContent.length > 280;

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 shadow-xs p-4 md:p-5 mb-4 transition-all duration-200 hover:shadow-md">
      {/* 1. Card Header */}
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-3">
          <div className="relative">
            <img
              src={
                actor.avatarUrl ||
                'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120'
              }
              alt={actor.fullName}
              className="w-10 h-10 md:w-11 md:h-11 rounded-full object-cover ring-2 ring-slate-100 dark:ring-slate-800"
            />
            {actor.role && (
              <span
                className={`absolute -bottom-1 -right-1 text-[9px] font-bold px-1.5 py-0.2 rounded-full border border-white dark:border-slate-900 ${
                  actor.role === 'LECTURER'
                    ? 'bg-amber-500 text-white'
                    : actor.role === 'ADMIN'
                    ? 'bg-rose-500 text-white'
                    : 'bg-blue-500 text-white'
                }`}
              >
                {actor.role === 'LECTURER' ? 'GV' : actor.role === 'ADMIN' ? 'AD' : 'SV'}
              </span>
            )}
          </div>

          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-bold text-slate-900 dark:text-white text-sm md:text-base">
                {actor.fullName}
              </span>
              {actor.schoolFaculty && (
                <span className="text-[11px] text-slate-500 dark:text-slate-400 hidden sm:inline">
                  • {actor.schoolFaculty}
                </span>
              )}
            </div>

            <div className="flex items-center gap-2 text-xs text-slate-400 mt-0.5">
              <span>
                {new Date(createdAt).toLocaleDateString('vi-VN', {
                  day: '2-digit',
                  month: '2-digit',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </span>
              <span>•</span>
              {content.visibility === 'PUBLIC' && (
                <span className="flex items-center gap-1" title="Công khai">
                  <Globe className="w-3 h-3 text-slate-400" />
                </span>
              )}
              {content.visibility === 'FRIENDS' && (
                <span className="flex items-center gap-1" title="Bạn bè">
                  <Users className="w-3 h-3 text-slate-400" />
                </span>
              )}
              {content.visibility === 'ONLY_ME' && (
                <span className="flex items-center gap-1" title="Chỉ mình tôi">
                  <Lock className="w-3 h-3 text-slate-400" />
                </span>
              )}
            </div>
          </div>
        </div>

        {/* Top-Right Menu & Subject Tag */}
        <div className="flex items-center gap-2 relative">
          {content.subject_name && (
            <div className="hidden sm:flex items-center gap-1 px-2.5 py-1 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 font-semibold text-xs border border-blue-200 dark:border-blue-900">
              <Tag className="w-3 h-3 text-blue-500" />
              <span>
                {content.subject_code ? `[${content.subject_code}] ` : ''}
                {content.subject_name}
              </span>
            </div>
          )}

          <div className="relative">
            <button
              type="button"
              onClick={() => setShowMenu(!showMenu)}
              className="p-1.5 rounded-full hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 transition-colors cursor-pointer"
            >
              <MoreHorizontal className="w-5 h-5" />
            </button>

            {showMenu && (
              <div className="absolute right-0 top-8 z-30 w-48 bg-white dark:bg-slate-800 rounded-2xl shadow-xl border border-slate-200 dark:border-slate-700 py-1.5 animate-in fade-in zoom-in-95 duration-150">
                {isLecturerOrAdmin && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowMenu(false);
                      setIsLecturerModalOpen(true);
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-xs font-semibold text-amber-600 dark:text-amber-400 hover:bg-amber-50 dark:hover:bg-amber-950/30 cursor-pointer"
                  >
                    <Award className="w-3.5 h-3.5" />
                    <span>Nhận xét của Giảng viên</span>
                  </button>
                )}

                {(isAuthor || user?.role === 'ADMIN') && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowMenu(false);
                      handleDelete();
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-xs font-semibold text-red-600 dark:text-red-400 hover:bg-red-50 dark:hover:bg-red-950/30 cursor-pointer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Xóa bài viết</span>
                  </button>
                )}

                <button
                  type="button"
                  onClick={() => {
                    setShowMenu(false);
                    handleShare();
                  }}
                  className="w-full flex items-center gap-2 px-3 py-2 text-xs text-slate-700 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 cursor-pointer"
                >
                  <Share2 className="w-3.5 h-3.5" />
                  <span>Chia sẻ liên kết</span>
                </button>

                {!isAuthor && (
                  <button
                    type="button"
                    onClick={() => {
                      setShowMenu(false);
                      setIsReportModalOpen(true);
                    }}
                    className="w-full flex items-center gap-2 px-3 py-2 text-xs font-semibold text-rose-600 dark:text-rose-400 hover:bg-rose-50 dark:hover:bg-rose-950/30 border-t border-slate-100 dark:border-slate-700 cursor-pointer"
                  >
                    <ShieldAlert className="w-3.5 h-3.5" />
                    <span>Báo cáo bài viết</span>
                  </button>
                )}
              </div>
            )}
          </div>
        </div>
      </div>

      {/* Mobile Subject Tag */}
      {content.subject_name && (
        <div className="sm:hidden flex items-center gap-1 mb-2.5 px-2.5 py-1 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 font-semibold text-xs border border-blue-200 dark:border-blue-900 w-fit">
          <Tag className="w-3 h-3 text-blue-500" />
          <span>
            {content.subject_code ? `[${content.subject_code}] ` : ''}
            {content.subject_name}
          </span>
        </div>
      )}

      {/* 2. Post Content */}
      <div className="mb-3.5">
        <p className="text-slate-800 dark:text-slate-100 text-sm md:text-base leading-relaxed whitespace-pre-wrap">
          {isLongText && !isContentExpanded
            ? `${textContent.slice(0, 280)}...`
            : textContent}
        </p>
        {isLongText && (
          <button
            type="button"
            onClick={() => setIsContentExpanded(!isContentExpanded)}
            className="text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline mt-1 cursor-pointer"
          >
            {isContentExpanded ? 'Thu gọn' : 'Xem thêm'}
          </button>
        )}
      </div>

      {/* 3. Media Attachments */}
      {content.mediaUrls && content.mediaUrls.length > 0 && (
        <div
          className={`grid gap-2 mb-3.5 rounded-2xl overflow-hidden ${
            content.mediaUrls.length === 1 ? 'grid-cols-1 max-h-[460px]' : 'grid-cols-2 max-h-[380px]'
          }`}
        >
          {content.mediaUrls.map((media, idx) => (
            <div key={idx} className="relative overflow-hidden bg-slate-100 dark:bg-slate-800 rounded-xl">
              {media.mediaType === 'VIDEO' ? (
                <video src={media.url} controls className="w-full h-full object-cover" />
              ) : (
                <img
                  src={media.url}
                  alt="post attachment"
                  className="w-full h-full object-cover hover:scale-102 transition-transform duration-300"
                />
              )}
            </div>
          ))}
        </div>
      )}

      {/* 4. Lecturer Note Card (Academic Distinction) */}
      {content.lecturerNote && (
        <div className="bg-gradient-to-r from-amber-500/10 via-amber-500/5 to-transparent dark:from-amber-950/40 dark:via-slate-800/40 dark:to-transparent rounded-2xl border-l-4 border-amber-500 p-3.5 mb-3.5">
          <div className="flex items-center justify-between mb-1.5">
            <div className="flex items-center gap-1.5 text-amber-700 dark:text-amber-400 font-bold text-xs">
              <Award className="w-4 h-4 text-amber-500" />
              <span>Ghi chú chuyên môn từ Giảng viên</span>
              {content.notedLecturer && (
                <span className="font-normal text-slate-500 dark:text-slate-400">
                  ({content.notedLecturer.fullName})
                </span>
              )}
            </div>

            {isLecturerOrAdmin && (
              <button
                type="button"
                onClick={() => setIsLecturerModalOpen(true)}
                className="text-[11px] font-semibold text-amber-600 hover:underline cursor-pointer"
              >
                Sửa
              </button>
            )}
          </div>
          <p className="text-xs md:text-sm text-slate-800 dark:text-slate-200 italic leading-relaxed">
            "{content.lecturerNote}"
          </p>
        </div>
      )}

      {/* 5. Post Actions (Like, Comment, Share, Lecturer Note quick button) */}
      <div className="flex items-center justify-between pt-2 border-t border-slate-100 dark:border-slate-800 text-xs md:text-sm text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-1 md:gap-2">
          {/* Reaction Button */}
          <button
            type="button"
            onClick={handleLike}
            className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl transition-all active:scale-95 cursor-pointer ${
              isLiked
                ? 'text-rose-600 font-bold bg-rose-50 dark:bg-rose-950/30'
                : 'hover:text-rose-600 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
          >
            <Heart className={`w-4 h-4 ${isLiked ? 'fill-current text-rose-600' : ''}`} />
            <span>{likeCount}</span>
          </button>

          {/* Comment Toggle Button */}
          <button
            type="button"
            onClick={() => setShowComments(!showComments)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl hover:text-blue-600 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <MessageCircle className="w-4 h-4" />
            <span>{content.commentCount || 0}</span>
            <span className="hidden sm:inline">bình luận</span>
          </button>

          {/* Share Button */}
          <button
            type="button"
            onClick={handleShare}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl hover:text-slate-900 dark:hover:text-white hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <Share2 className="w-4 h-4" />
          </button>
        </div>

        {/* Quick button for Lecturer if note not yet added */}
        {isLecturerOrAdmin && !content.lecturerNote && (
          <button
            type="button"
            onClick={() => setIsLecturerModalOpen(true)}
            className="flex items-center gap-1 px-2.5 py-1 rounded-xl text-xs font-semibold text-amber-700 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 hover:bg-amber-100 dark:hover:bg-amber-900/50 transition-colors cursor-pointer"
          >
            <Award className="w-3.5 h-3.5 text-amber-500" />
            <span>+ Ghi chú GV</span>
          </button>
        )}
      </div>

      {/* 6. Comment Drawer */}
      {showComments && <CommentSection targetType="POST" targetId={targetId} />}

      {/* 7. Lecturer Note Modal */}
      <LecturerNoteModal
        postId={targetId}
        initialNote={content.lecturerNote}
        isOpen={isLecturerModalOpen}
        onClose={() => setIsLecturerModalOpen(false)}
      />

      {/* 8. Report Modal */}
      <ReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        targetType="POST"
        targetId={targetId}
        targetTitle={`bài viết của ${actor.fullName || 'người dùng'}`}
      />
    </div>
  );
};
