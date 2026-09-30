import { useState } from 'react';
import type { PostResponseDto, CommentResponseDto } from '@/types/post.types';
import { CommentSection } from './CommentSection';
import {
  Heart,
  MessageSquare,
  Share2,
  Bookmark,
  GraduationCap,
  Sparkles,
  Trophy,
  MoreHorizontal,
  Globe,
  Users,
  Lock,
  BookOpen
} from 'lucide-react';

interface PostCardProps {
  post: PostResponseDto;
  onToggleLike: (postId: number) => void;
  onAddComment: (postId: number, content: string, parentCommentId?: number) => void;
}

export function PostCard({ post, onToggleLike, onAddComment }: PostCardProps) {
  const [showComments, setShowComments] = useState(false);
  const [isBookmarked, setIsBookmarked] = useState(false);

  const isSystemRewardPost = post.postType === 'LEADERBOARD_HONOR';

  // Mock comments cho demo nếu chưa có
  const [comments, setComments] = useState<CommentResponseDto[]>(
    post.commentCount > 0
      ? [
          {
            commentId: 1001,
            targetType: 'POST',
            targetId: post.postId,
            content: 'Bài giải thích phần này rất trực quan, cảm ơn bạn nhiều nhé!',
            author: {
              userId: 201,
              fullName: 'Trần Thị Bình',
              role: 'STUDENT',
            },
            createdAt: '10 phút trước',
            replyCount: 1,
            replies: [
              {
                commentId: 1002,
                targetType: 'POST',
                targetId: post.postId,
                parentCommentId: 1001,
                content: 'Mình có bổ sung thêm tài liệu tham khảo ở link bên dưới nhé.',
                author: post.author,
                createdAt: '5 phút trước',
                replyCount: 0,
                replies: [],
              },
            ],
          },
        ]
      : []
  );

  const handleAddCommentLocal = (postId: number, content: string, parentCommentId?: number) => {
    const newComment: CommentResponseDto = {
      commentId: Date.now(),
      targetType: 'POST',
      targetId: postId,
      parentCommentId,
      content,
      author: {
        userId: 999,
        fullName: 'Bạn (Sinh viên)',
        role: 'STUDENT',
      },
      createdAt: 'Vừa xong',
      replyCount: 0,
      replies: [],
    };

    if (parentCommentId) {
      setComments(
        comments.map((c) =>
          c.commentId === parentCommentId
            ? { ...c, replies: [...(c.replies || []), newComment] }
            : c
        )
      );
    } else {
      setComments([newComment, ...comments]);
    }
    onAddComment(postId, content, parentCommentId);
  };

  return (
    <article
      className={`rounded-2xl border transition-shadow duration-200 shadow-xs ${
        isSystemRewardPost
          ? 'border-amber-300 dark:border-amber-500/40 bg-linear-to-b from-amber-50/40 via-white to-white dark:from-amber-950/20 dark:via-slate-900 dark:to-slate-900'
          : 'border-slate-200/80 dark:border-slate-800 bg-white dark:bg-slate-900 hover:shadow-md'
      }`}
    >
      <div className="p-4 sm:p-5 space-y-3.5">
        
        {/* Post Header */}
        <div className="flex items-start justify-between gap-3">
          <div className="flex items-center gap-3">
            
            {/* Avatar with optional frame */}
            <div className="relative shrink-0">
              <div
                className={`w-10 h-10 sm:w-11 sm:h-11 rounded-full flex items-center justify-center font-bold text-sm shadow-xs overflow-hidden ${
                  isSystemRewardPost
                    ? 'bg-linear-to-tr from-amber-400 to-yellow-600 text-white'
                    : 'bg-linear-to-tr from-indigo-500 to-violet-600 text-white'
                }`}
              >
                {isSystemRewardPost ? (
                  <Trophy className="w-5 h-5 text-white" />
                ) : post.author.avatarUrl ? (
                  <img src={post.author.avatarUrl} alt={post.author.fullName} className="w-full h-full object-cover" />
                ) : (
                  post.author.fullName.charAt(0)
                )}
              </div>

              {post.author.frameUrl && !isSystemRewardPost && (
                <img
                  src={post.author.frameUrl}
                  alt="Frame"
                  className="absolute -inset-1.5 w-[calc(100%+12px)] h-[calc(100%+12px)] object-contain pointer-events-none"
                />
              )}
            </div>

            {/* Author info & Metadata */}
            <div>
              <div className="flex flex-wrap items-center gap-2">
                <h4 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100 leading-tight">
                  {post.author.fullName}
                </h4>

                {/* Role badge */}
                {post.author.role === 'LECTURER' && (
                  <span className="text-[10px] font-bold bg-emerald-100 dark:bg-emerald-950/50 text-emerald-700 dark:text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-200 dark:border-emerald-800/60 flex items-center gap-1">
                    <GraduationCap className="w-3 h-3" />
                    Giảng viên
                  </span>
                )}
                {isSystemRewardPost && (
                  <span className="text-[10px] font-extrabold bg-amber-100 dark:bg-amber-950/50 text-amber-800 dark:text-amber-300 px-2 py-0.5 rounded-full border border-amber-300 dark:border-amber-700/60 flex items-center gap-1">
                    <Sparkles className="w-3 h-3 text-amber-600 dark:text-amber-400" />
                    Vinh danh SQB
                  </span>
                )}
              </div>

              <div className="flex flex-wrap items-center gap-1.5 text-xs text-slate-400 dark:text-slate-500 mt-0.5">
                <span>{post.createdAt}</span>
                <span>•</span>
                {post.visibility === 'FRIENDS' ? (
                  <span className="inline-flex items-center gap-1 text-indigo-600 dark:text-indigo-400 font-medium" title="Chế độ: Bạn bè">
                    <Users className="w-3 h-3" />
                    <span className="text-[10px]">Bạn bè</span>
                  </span>
                ) : post.visibility === 'ONLY_ME' ? (
                  <span className="inline-flex items-center gap-1 text-amber-600 dark:text-amber-400 font-medium" title="Chế độ: Chỉ mình tôi">
                    <Lock className="w-3 h-3" />
                    <span className="text-[10px]">Chỉ mình tôi</span>
                  </span>
                ) : (
                  <span className="inline-flex items-center gap-1 text-slate-400 dark:text-slate-500" title="Chế độ: Công khai">
                    <Globe className="w-3 h-3" />
                    <span className="text-[10px]">Công khai</span>
                  </span>
                )}

                {/* Subject Tag if attached */}
                {post.subjectCode && (
                  <>
                    <span>•</span>
                    <span className="inline-flex items-center gap-1 text-indigo-600 dark:text-indigo-400 font-semibold bg-indigo-50 dark:bg-indigo-950/50 px-2 py-0.5 rounded-md hover:bg-indigo-100 dark:hover:bg-indigo-900/60 border border-indigo-100 dark:border-indigo-800/40 transition-colors cursor-pointer">
                      <BookOpen className="w-3 h-3" />
                      {post.subjectCode} • {post.subjectName}
                    </span>
                  </>
                )}
              </div>
            </div>
          </div>

          {/* More Action Menu */}
          {/* TODO: Khi xóa bài viết (DELETE /posts/{id}): backend trả về DeletePostResponseDto chứa postId để FE loại bỏ post khỏi cache/danh sách */}
          <button
            type="button"
            className="p-1.5 text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 rounded-lg transition-colors cursor-pointer"
          >
            <MoreHorizontal className="w-4 h-4" />
          </button>
        </div>

        {/* Post Content */}
        <div className="text-sm text-slate-800 dark:text-slate-200 leading-relaxed space-y-2 whitespace-pre-line">
          {post.content}
        </div>

        {/* Media Attachments Grid (1, 2, 3, 4 images) */}
        {post.mediaUrls && post.mediaUrls.length > 0 && (
          <div
            className={`grid gap-2 rounded-2xl overflow-hidden ${
              post.mediaUrls.length === 1
                ? 'grid-cols-1'
                : post.mediaUrls.length === 2
                ? 'grid-cols-2'
                : 'grid-cols-2 sm:grid-cols-3'
            }`}
          >
            {post.mediaUrls.map((media, idx) => (
              <div
                key={idx}
                className="relative bg-slate-100 dark:bg-slate-800 overflow-hidden aspect-video group cursor-pointer"
              >
                <img
                  src={media.url}
                  alt={`post-media-${idx}`}
                  className="w-full h-full object-cover group-hover:scale-102 transition-transform duration-200"
                />
              </div>
            ))}
          </div>
        )}

        {/* Lecturer Note Callout (Academic Highlight Feature) */}
        {post.lecturerNote && (
          <div className="p-3.5 sm:p-4 rounded-xl bg-linear-to-r from-emerald-50 to-teal-50 dark:from-emerald-950/30 dark:to-teal-950/30 border border-emerald-200 dark:border-emerald-800/50 text-emerald-900 dark:text-emerald-100 space-y-1.5 shadow-2xs">
            <div className="flex items-center gap-2 text-xs font-bold text-emerald-800 dark:text-emerald-300">
              <div className="w-6 h-6 rounded-lg bg-emerald-600 text-white flex items-center justify-center">
                <GraduationCap className="w-3.5 h-3.5" />
              </div>
              <span>
                Ghi chú học thuật từ {post.notedLecturer?.fullName || 'Giảng viên chuyên môn'}:
              </span>
            </div>
            <p className="text-xs sm:text-sm text-emerald-950 dark:text-emerald-200 pl-8 leading-relaxed italic">
              "{post.lecturerNote}"
            </p>
          </div>
        )}

        {/* Actions Bar (Like, Comment, Share, Bookmark) */}
        <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-1 sm:gap-2">
            
            {/* Like Button */}
            {/* TODO: Nâng cấp nút Like đơn thuần thành Reaction Popover hỗ trợ 3 cảm xúc tích cực: LIKE (Đồng tình), LOVE (Bổ ích), WOW (Khâm phục) */}
            <button
              type="button"
              onClick={() => onToggleLike(post.postId)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-semibold transition-all cursor-pointer ${
                post.reactedByMe
                  ? 'text-rose-600 dark:text-rose-400 bg-rose-50 dark:bg-rose-950/40 hover:bg-rose-100/80 dark:hover:bg-rose-900/40'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              <Heart
                className={`w-4 h-4 transition-transform active:scale-125 ${
                  post.reactedByMe ? 'fill-rose-500 text-rose-500' : ''
                }`}
              />
              <span>{post.reactCount}</span>
            </button>

            {/* Comment Button */}
            <button
              type="button"
              onClick={() => setShowComments(!showComments)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-semibold transition-colors cursor-pointer ${
                showComments
                  ? 'text-indigo-600 dark:text-indigo-400 bg-indigo-50 dark:bg-indigo-950/40'
                  : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200'
              }`}
            >
              <MessageSquare className="w-4 h-4" />
              <span>{post.commentCount + (comments.length > post.commentCount ? comments.length - post.commentCount : 0)}</span>
            </button>

            {/* Share Button */}
            <button
              type="button"
              className="hidden sm:flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-semibold text-slate-600 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-900 dark:hover:text-slate-200 transition-colors cursor-pointer"
            >
              <Share2 className="w-4 h-4" />
              <span>Chia sẻ</span>
            </button>
          </div>

          {/* Bookmark Button */}
          <button
            type="button"
            onClick={() => setIsBookmarked(!isBookmarked)}
            className={`p-2 rounded-xl transition-colors cursor-pointer ${
              isBookmarked
                ? 'text-amber-500 bg-amber-50 dark:bg-amber-950/40'
                : 'text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800'
            }`}
            title="Lưu trữ bài viết"
          >
            <Bookmark className={`w-4 h-4 ${isBookmarked ? 'fill-amber-500' : ''}`} />
          </button>
        </div>

        {/* Expandable Comment Section */}
        {showComments && (
          <CommentSection
            postId={post.postId}
            comments={comments}
            onAddComment={handleAddCommentLocal}
          />
        )}

      </div>
    </article>
  );
}
