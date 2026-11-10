import React, { useState, useRef } from 'react';
import { Menu } from '@base-ui/react/menu';
import { Link } from 'react-router-dom';
import { Badge } from '@/components/ui/badge';
import { Button } from '@/components/ui/button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useAuthStore } from '@/stores/useAuthStore';
import { usePostReact } from '../hooks/usePostReact';
import { usePostMutations } from '../hooks/usePostMutations';
import { CommentSection } from './CommentSection';
import { LecturerNoteModal } from './LecturerNoteModal';
import { ReportModal } from '@/features/report';
import type { ActivityFeedItemDto } from '../types/feed.types';
import type { ReactionType } from '../services/reactService';
import { useToastStore } from '@/stores/useToastStore';
import {
  ThumbsUp,
  Heart,
  Sparkles,
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
  ArrowRight,
  CheckCircle2,
} from 'lucide-react';

const REACTIONS = [
  {
    type: 'LIKE' as ReactionType,
    label: 'Thích',
    icon: ThumbsUp,
    color: 'text-blue-600 dark:text-blue-400',
    bgColor: 'bg-blue-50 dark:bg-blue-950/40',
    ringColor: 'ring-blue-300 dark:ring-blue-800',
    emoji: '👍',
  },
  {
    type: 'LOVE' as ReactionType,
    label: 'Yêu thích',
    icon: Heart,
    color: 'text-rose-600 dark:text-rose-400',
    bgColor: 'bg-rose-50 dark:bg-rose-950/40',
    ringColor: 'ring-rose-300 dark:ring-rose-800',
    emoji: '❤️',
  },
  {
    type: 'WOW' as ReactionType,
    label: 'Tuyệt vời',
    icon: Sparkles,
    color: 'text-amber-500 dark:text-amber-400',
    bgColor: 'bg-amber-50 dark:bg-amber-950/40',
    ringColor: 'ring-amber-300 dark:ring-amber-800',
    emoji: '😮',
  },
];

const postMenuItemClass = 'flex min-h-11 cursor-pointer items-center gap-2 rounded-lg px-3 text-sm outline-none data-highlighted:bg-accent focus-visible:ring-2 focus-visible:ring-inset focus-visible:ring-ring';

interface PostFeedCardProps {
  item: ActivityFeedItemDto;
  initiallyShowComments?: boolean;
  onDeleted?: () => void;
}

export const PostFeedCard: React.FC<PostFeedCardProps> = ({
  item,
  initiallyShowComments = false,
  onDeleted,
}) => {
  const { user } = useAuthStore();
  const { toggleReact } = usePostReact();
  const { deletePost } = usePostMutations();
  const { addToast } = useToastStore();

  const { actor, content, createdAt, targetId } = item;

  const myReaction: ReactionType | null =
    (content.myReaction as ReactionType) || (content.reactedByMe ? 'LIKE' : null);
  const likeCount = content.reactCount || 0;
  const currentReactionConfig = REACTIONS.find((r) => r.type === myReaction);

  const [showComments, setShowComments] = useState<boolean>(initiallyShowComments);
  const [showReactionPicker, setShowReactionPicker] = useState<boolean>(false);
  const pickerTimeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const [isLecturerModalOpen, setIsLecturerModalOpen] = useState<boolean>(false);
  const [isReportModalOpen, setIsReportModalOpen] = useState<boolean>(false);
  const [isContentExpanded, setIsContentExpanded] = useState<boolean>(false);

  const isApprovalPost = content.postType === 'QUESTION_APPROVED_NOTIFICATION';
  const isAuthor = !isApprovalPost && user?.id != null && user.id === actor.userId;
  const canEditLecturerNote = isApprovalPost && content.canEditLecturerNote === true;

  const handleMouseEnterPicker = () => {
    if (pickerTimeoutRef.current) {
      clearTimeout(pickerTimeoutRef.current);
      pickerTimeoutRef.current = null;
    }
    setShowReactionPicker(true);
  };

  const handleMouseLeavePicker = () => {
    pickerTimeoutRef.current = setTimeout(() => {
      setShowReactionPicker(false);
    }, 250);
  };

  const handleSelectReaction = (type: ReactionType) => {
    setShowReactionPicker(false);
    toggleReact({
      targetType: 'POST',
      targetId,
      reactionType: type,
    });
  };

  const handleMainButtonClick = () => {
    // Nếu đang có reaction thì bấm vào để hủy, nếu chưa có thì mặc định thả LIKE
    const typeToToggle: ReactionType = myReaction || 'LIKE';
    toggleReact({
      targetType: 'POST',
      targetId,
      reactionType: typeToToggle,
    });
  };

  const handleShare = () => {
    navigator.clipboard.writeText(`${window.location.origin}/posts/${targetId}`);
    addToast({
      type: 'info',
      title: 'Đã sao chép liên kết',
      message: 'Đường dẫn bài viết đã được lưu vào bộ nhớ tạm.',
    });
  };

  const handleDelete = async () => {
    if (window.confirm('Bạn có chắc chắn muốn xóa bài viết này không?')) {
      try {
        await deletePost(targetId);
        onDeleted?.();
      } catch {
        // Mutation đã hiển thị thông báo lỗi; giữ nguyên trang để người dùng có thể thử lại.
      }
    }
  };

  const textContent = content.description || content.title || '';
  const isLongText = textContent.length > 280;

  return (
    <article className={`mb-4 rounded-2xl border bg-card p-4 shadow-[var(--card-shadow)] md:p-5 ${
      isApprovalPost ? 'border-academic/30' : 'border-border'
    }`}>
      {/* 1. Card Header */}
      <div className="mb-3 flex items-center justify-between gap-2">
        <div className="flex min-w-0 flex-1 items-center gap-3">
          <div className="relative shrink-0">
            <UserAvatar
              avatarUrl={actor.avatarUrl}
              name={actor.fullName}
              className="size-11 ring-2 ring-muted"
              decorative
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

          <div className="min-w-0">
            <div className="flex items-baseline gap-1.5">
              <span className="truncate text-sm font-bold text-foreground md:text-base" title={actor.fullName}>
                {actor.fullName}
              </span>
              {actor.schoolFaculty && (
                <span className="hidden max-w-[35%] truncate text-[11px] text-muted-foreground sm:inline" title={actor.schoolFaculty}>
                  • {actor.schoolFaculty}
                </span>
              )}
            </div>

            <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
              <span className="shrink-0">
                {new Date(createdAt).toLocaleDateString('vi-VN', {
                  day: '2-digit',
                  month: '2-digit',
                  hour: '2-digit',
                  minute: '2-digit',
                })}
              </span>
              {content.visibility && <span aria-hidden="true">•</span>}
              {content.visibility === 'PUBLIC' && (
                <span className="flex shrink-0 items-center gap-1" title="Công khai">
                  <Globe className="w-3 h-3 text-slate-400" />
                </span>
              )}
              {content.visibility === 'FRIENDS' && (
                <span className="flex shrink-0 items-center gap-1" title="Bạn bè">
                  <Users className="w-3 h-3 text-slate-400" />
                </span>
              )}
              {content.visibility === 'ONLY_ME' && (
                <span className="flex shrink-0 items-center gap-1" title="Chỉ mình tôi">
                  <Lock className="w-3 h-3 text-slate-400" />
                </span>
              )}
              {item.actionType === 'SESSION_RESOLVED_APPROVED' && (
                <>
                  <span aria-hidden="true">•</span>
                  <span className="truncate" title="vừa đóng góp thành công">vừa đóng góp thành công</span>
                </>
              )}
            </div>
          </div>
        </div>

        {/* Top-Right Menu & Subject Tag */}
        <div className="relative flex shrink-0 items-center gap-2">
          {content.subjectName && (
            <div className="hidden max-w-44 items-center gap-1 rounded-full border border-blue-200 bg-blue-50 px-2.5 py-1 text-xs font-semibold text-blue-700 sm:flex lg:max-w-60 dark:border-blue-900 dark:bg-blue-950/40 dark:text-blue-300" title={content.subjectName}>
              <Tag className="size-3 shrink-0 text-blue-500" />
              <span className="truncate">
                {content.subjectCode ? `[${content.subjectCode}] ` : ''}
                {content.subjectName}
              </span>
            </div>
          )}

          <Menu.Root modal={false}>
            <Menu.Trigger
              render={<Button variant="ghost" size="icon" className="size-11 text-muted-foreground" />}
              aria-label="Mở tùy chọn bài viết"
            >
              <MoreHorizontal aria-hidden="true" />
            </Menu.Trigger>
            <Menu.Portal>
              <Menu.Positioner align="end" sideOffset={6} className="z-50 max-w-(--available-width)">
                <Menu.Popup className="w-56 max-w-(--available-width) overflow-hidden rounded-xl border border-border bg-popover p-1 text-popover-foreground shadow-lg">
                  {canEditLecturerNote && (
                    <Menu.Item className={`${postMenuItemClass} font-semibold text-warning`} onClick={() => setIsLecturerModalOpen(true)}>
                      <Award aria-hidden="true" className="size-4 shrink-0" /> Nhận xét của Giảng viên
                    </Menu.Item>
                  )}

                  {(isAuthor || user?.role === 'ADMIN') && (
                    <Menu.Item className={`${postMenuItemClass} font-semibold text-destructive`} onClick={() => void handleDelete()}>
                      <Trash2 aria-hidden="true" className="size-4 shrink-0" /> Xóa bài viết
                    </Menu.Item>
                  )}

                  <Menu.Item className={postMenuItemClass} onClick={handleShare}>
                    <Share2 aria-hidden="true" className="size-4 shrink-0" /> Chia sẻ liên kết
                  </Menu.Item>

                  {!isAuthor && (
                    <>
                      <Menu.Separator className="my-1 h-px bg-border" />
                      <Menu.Item className={`${postMenuItemClass} font-semibold text-destructive`} onClick={() => setIsReportModalOpen(true)}>
                        <ShieldAlert aria-hidden="true" className="size-4 shrink-0" /> Báo cáo bài viết
                      </Menu.Item>
                    </>
                  )}
                </Menu.Popup>
              </Menu.Positioner>
            </Menu.Portal>
          </Menu.Root>
        </div>
      </div>

      {/* Mobile Subject Tag */}
      {content.subjectName && (
        <div className="sm:hidden flex items-center gap-1 mb-2.5 px-2.5 py-1 rounded-full bg-blue-50 dark:bg-blue-950/40 text-blue-700 dark:text-blue-300 font-semibold text-xs border border-blue-200 dark:border-blue-900 w-fit">
          <Tag className="w-3 h-3 text-blue-500" />
          <span>
            {content.subjectCode ? `[${content.subjectCode}] ` : ''}
            {content.subjectName}
          </span>
        </div>
      )}

      {/* 2. Post Content */}
      <div className={isApprovalPost ? 'mb-3.5 space-y-3 rounded-xl border border-academic/20 bg-academic-soft p-4' : 'mb-3.5'}>
        {isApprovalPost && (
          <Badge variant="outline" className="border-academic/25 text-academic">
            <CheckCircle2 aria-hidden="true" /> Đã duyệt vào ngân hàng
          </Badge>
        )}
        <p className="whitespace-pre-wrap text-sm leading-6 text-foreground md:text-base md:leading-7">
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
        {isApprovalPost && content.sessionId != null && (
          <Button nativeButton={false} render={<Link to={`/sessions/${content.sessionId}/practice`} />} className="min-h-11">
            Luyện tập ngay <ArrowRight aria-hidden="true" />
          </Button>
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
            <div key={idx} className="relative min-h-48 overflow-hidden rounded-xl bg-muted">
              {media.mediaType === 'VIDEO' ? (
                <video src={media.url} controls className="w-full h-full object-cover" />
              ) : (
                <img
                  src={media.url}
                  alt="post attachment"
                  className="w-full h-full object-cover hover:scale-102 transition-transform duration-300"
                  loading="lazy"
                />
              )}
            </div>
          ))}
        </div>
      )}

      {/* 4. Lecturer Note Card (Academic Distinction) */}
      {isApprovalPost && content.lecturerNote && (
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

            {canEditLecturerNote && (
              <button
                type="button"
                onClick={() => setIsLecturerModalOpen(true)}
                className="min-h-11 px-2 text-xs font-semibold text-amber-700 hover:underline dark:text-amber-400 cursor-pointer"
              >
                Sửa
              </button>
            )}
          </div>
          <p className="wrap-break-word whitespace-pre-wrap text-xs md:text-sm text-slate-800 dark:text-slate-200 italic leading-relaxed">
            "{content.lecturerNote}"
          </p>
        </div>
      )}

      {/* 5. Post Actions (Like, Comment, Share, Lecturer Note quick button) */}
      <div className="flex flex-wrap items-center justify-between gap-2 border-t border-border pt-2 text-sm text-muted-foreground">
        <div className="flex items-center gap-1 md:gap-2">
          {/* Reaction Button with Floating Popover Picker (LIKE, LOVE, WOW) */}
          <div
            className="relative"
            onMouseEnter={handleMouseEnterPicker}
            onMouseLeave={handleMouseLeavePicker}
          >
            {/* Floating Reaction Bar (Facebook / LinkedIn Style) */}
            {showReactionPicker && (
              <div
                className="absolute bottom-full left-0 mb-2 z-30 flex items-center gap-1.5 p-1.5 rounded-full bg-white/95 dark:bg-slate-900/95 backdrop-blur-md shadow-xl border border-slate-200/80 dark:border-slate-700 animate-in fade-in zoom-in-95 duration-150"
                onMouseEnter={handleMouseEnterPicker}
                onMouseLeave={handleMouseLeavePicker}
              >
                {REACTIONS.map((r) => {
                  const isSelected = myReaction === r.type;
                  return (
                    <button
                      key={r.type}
                      type="button"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleSelectReaction(r.type);
                      }}
                      className={`group/btn relative flex items-center justify-center w-9 h-9 rounded-full transition-all duration-200 hover:scale-125 active:scale-95 cursor-pointer ${
                        isSelected
                          ? `${r.bgColor} ring-2 ${r.ringColor}`
                          : 'hover:bg-slate-100 dark:hover:bg-slate-800'
                      }`}
                      title={r.label}
                    >
                      <span className="text-xl leading-none select-none drop-shadow-xs">{r.emoji}</span>
                      <span className="absolute -top-7 px-1.5 py-0.5 rounded-md bg-slate-900 text-white text-[10px] font-medium opacity-0 group-hover/btn:opacity-100 transition-opacity whitespace-nowrap pointer-events-none shadow-md">
                        {r.label}
                      </span>
                    </button>
                  );
                })}
              </div>
            )}

            <button
              type="button"
              onClick={handleMainButtonClick}
              className={`flex min-h-11 items-center gap-1.5 rounded-xl px-3 transition-colors ${
                currentReactionConfig
                  ? `${currentReactionConfig.color} font-bold ${currentReactionConfig.bgColor}`
                  : 'hover:text-blue-600 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-500 dark:text-slate-400'
              }`}
            >
              {currentReactionConfig ? (
                <>
                  <span className="text-base leading-none select-none">{currentReactionConfig.emoji}</span>
                  <span>{currentReactionConfig.label}</span>
                </>
              ) : (
                <>
                  <ThumbsUp className="w-4 h-4" />
                  <span>Thích</span>
                </>
              )}
              {likeCount > 0 && <span className="text-xs font-semibold">{likeCount}</span>}
            </button>
          </div>

          {/* Comment Toggle Button */}
          <button
            type="button"
            onClick={() => setShowComments(!showComments)}
            className="flex min-h-11 items-center gap-1.5 rounded-xl px-3 transition-colors hover:bg-muted hover:text-primary"
          >
            <MessageCircle className="w-4 h-4" />
            <span>{content.commentCount || 0}</span>
            <span className="hidden sm:inline">bình luận</span>
          </button>

          {/* Share Button */}
          <button
            type="button"
            onClick={handleShare}
            className="flex min-h-11 items-center gap-1.5 rounded-xl px-3 transition-colors hover:bg-muted hover:text-foreground"
          >
            <Share2 className="w-4 h-4" />
          </button>
        </div>

        {/* Quick button for Lecturer if note not yet added */}
        {canEditLecturerNote && !content.lecturerNote && (
          <button
            type="button"
            onClick={() => setIsLecturerModalOpen(true)}
            className="flex min-h-11 items-center gap-1 rounded-xl bg-warning-soft px-3 text-xs font-semibold text-warning transition-colors hover:bg-muted focus-visible:outline-2 focus-visible:outline-ring"
          >
            <Award className="w-3.5 h-3.5 text-amber-500" />
            <span>+ Ghi chú GV</span>
          </button>
        )}
      </div>

      {/* 6. Comment Drawer */}
      {showComments && <CommentSection targetType="POST" targetId={targetId} />}

      {/* 7. Lecturer Note Modal */}
      {canEditLecturerNote && isLecturerModalOpen && (
        <LecturerNoteModal postId={targetId} initialNote={content.lecturerNote} onClose={() => setIsLecturerModalOpen(false)} />
      )}

      {/* 8. Report Modal */}
      <ReportModal
        isOpen={isReportModalOpen}
        onClose={() => setIsReportModalOpen(false)}
        targetType="POST"
        targetId={targetId}
        targetTitle={`bài viết của ${actor.fullName || 'người dùng'}`}
      />
    </article>
  );
};
