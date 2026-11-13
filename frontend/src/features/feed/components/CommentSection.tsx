import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import type { CommentResponseDto } from '@/types/post.types';
import React, { useRef, useState } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useComments, useCommentReplies } from '../hooks/useComments';
import { mediaService } from '@/services/mediaService';
import { Button } from '@/components/ui/button';
import { UserAvatar } from '@/components/ui/user-avatar';
import { useCurrentUserAvatarName } from '@/features/profile/hooks/useCurrentUserAvatarName';
import {
  Send,
  Loader2,
  Trash2,
  CornerDownRight,
  Image as ImageIcon,
  X,
} from 'lucide-react';

interface CommentSectionProps {
  targetType: 'POST' | 'COMMENT' | 'SESSION';
  targetId: number;
}

export const CommentSection: React.FC<CommentSectionProps> = ({ targetType, targetId }) => {
  const { user } = useAuthStore();
  const avatarName = useCurrentUserAvatarName(user);
  const {
    comments,
    isLoading,
    isLoadingError,
    refetch,
    isCreating,
    createComment,
    deleteComment,
    commentsQuery,
  } = useComments(targetType, targetId);

  const commentsRoot = useRef<HTMLDivElement>(null);
  const [inputContent, setInputContent] = useState('');
  const [replyingToCommentId, setReplyingToCommentId] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [commentMediaUrl, setCommentMediaUrl] = useState<string | null>(null);
  const [isUploadingMedia, setIsUploadingMedia] = useState(false);

  const handleMediaUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setIsUploadingMedia(true);
    try {
      const { publicUrl } = await mediaService.uploadViaPresign(file, 'POST');
      setCommentMediaUrl(publicUrl);
    } finally {
      setIsUploadingMedia(false);
    }
  };

  const handleSubmitComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (isCreating || (!inputContent.trim() && !commentMediaUrl)) return;

    try {
      await createComment({ content: inputContent.trim(), mediaUrl: commentMediaUrl || undefined });
      setInputContent('');
      setCommentMediaUrl(null);
    } catch {
      // The mutation displays the error. Keep the draft for a retry.
    }
  };

  const handleReplySubmit = async (parentCommentId: number) => {
    if (isCreating || !replyContent.trim()) return;

    try {
      await createComment({ content: replyContent.trim(), parentCommentId });
      setReplyContent('');
      setReplyingToCommentId(null);
    } catch {
      // Keep the reply for a retry.
    }
  };

  if (isLoadingError) {
    return (
      <div className="space-y-3 py-6 text-center">
        <p role="status" className="text-sm text-muted-foreground">Không thể tải cuộc thảo luận.</p>
        <Button variant="outline" onClick={() => void refetch()} className="min-h-11">Thử lại</Button>
      </div>
    );
  }

  return (
      <section aria-label="Bình luận" className={targetType === 'SESSION' ? undefined : 'mt-3 border-t border-border pt-4'}>
          {/* 1. Comment Input Box */}
          <form
              onSubmit={handleSubmitComment}
              className="flex gap-2.5 items-start mb-4"
          >
              <UserAvatar
                  avatarUrl={user?.avatarUrl}
                  name={avatarName}
                  className="mt-0.5 size-8 text-xs"
              />

              <div className="flex-1 rounded-2xl border border-border bg-surface-subtle p-2 transition-[border-color,box-shadow] focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/30">
                  <textarea
                      aria-label="Nội dung bình luận"
                      value={inputContent}
                      onChange={(e) => setInputContent(e.target.value)}
                      placeholder={targetType === 'SESSION' ? 'Chia sẻ góp ý về cả bộ câu hỏi…' : 'Viết bình luận hoặc lời giải của bạn...'}
                      rows={1}
                      className="w-full resize-none bg-transparent px-1 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none"
                  />

                  {commentMediaUrl && (
                      <div className="relative inline-block mt-2">
                          <img
                              src={commentMediaUrl}
                              alt="comment media"
                              className="w-20 h-20 object-cover rounded-xl border border-slate-300 dark:border-slate-600"
                          />
                          <button
                              type="button"
                              onClick={() => setCommentMediaUrl(null)}
                              className="absolute -top-1.5 -right-1.5 p-1 bg-black/70 hover:bg-black text-white rounded-full"
                          >
                              <X className="w-3 h-3" />
                          </button>
                      </div>
                  )}

                  <div className="flex items-center justify-between pt-1.5 mt-1 border-t border-slate-200/60 dark:border-slate-700/60">
                      <label className="grid size-11 cursor-pointer place-items-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-primary" aria-label="Đính kèm ảnh">
                          <ImageIcon className="w-4 h-4" />
                          <input
                              type="file"
                              accept="image/*"
                              onChange={handleMediaUpload}
                              className="hidden"
                              disabled={isUploadingMedia}
                          />
                      </label>

                      <button
                          type="submit"
                          disabled={
                              isCreating ||
                              isUploadingMedia ||
                              (!inputContent.trim() && !commentMediaUrl)
                          }
                          className="flex min-h-11 items-center gap-1.5 rounded-xl bg-primary px-4 text-sm font-semibold text-primary-foreground shadow-[var(--shadow-primitive-xs)] transition-colors hover:bg-primary/85 disabled:opacity-40"
                      >
                          {isCreating || isUploadingMedia ? (
                              <Loader2 className="w-3.5 h-3.5 animate-spin" />
                          ) : (
                              <>
                                  <span>Gửi</span>
                                  <Send className="w-3 h-3" />
                              </>
                          )}
                      </button>
                  </div>
              </div>
          </form>

          {/* 2. Comments List */}
          {isLoading ? (
              <div className="flex items-center justify-center py-6" role="status">
                  <Loader2 className="size-5 animate-spin text-primary" />
                  <span className="sr-only">Đang tải bình luận</span>
              </div>
          ) : comments.length === 0 ? (
              <div className="py-5 text-center text-sm text-muted-foreground">
                  Chưa có bình luận nào. Hãy là người đầu tiên trao đổi!
              </div>
          ) : (
              <div ref={commentsRoot} className="max-h-[min(65vh,36rem)] space-y-3 overflow-y-auto overscroll-contain pr-1">
                  {comments.map((comment) => (
                      <div key={comment.commentId} className="group/item">
                          <div className="flex gap-2.5 items-start">
                              <UserAvatar
                                  avatarUrl={comment.author?.avatarUrl}
                                  name={comment.author?.fullName}
                                  className="mt-0.5 size-7 text-xs"
                                  decorative
                              />

                              <div className="flex-1">
                                  <div className="inline-block bg-slate-100 dark:bg-slate-800/80 rounded-2xl px-3 py-2 text-xs text-slate-800 dark:text-slate-200 border border-slate-200/60 dark:border-slate-700/60">
                                      <div className="flex items-center gap-2 mb-0.5">
                                          <span className="font-semibold text-slate-900 dark:text-white">
                                              {comment.author?.fullName ||
                                                  "Người dùng SQB"}
                                          </span>
                                          {comment.author?.role && (
                                              <span
                                                  className={`text-[9px] font-bold px-1.5 py-0.2 rounded-full ${
                                                      comment.author.role ===
                                                      "LECTURER"
                                                          ? "bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-400"
                                                          : "bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-400"
                                                  }`}
                                              >
                                                  {comment.author.role ===
                                                  "LECTURER"
                                                      ? "Giảng viên"
                                                      : "Sinh viên"}
                                              </span>
                                          )}
                                      </div>
                                      <p className="wrap-anywhere whitespace-pre-wrap">
                                          {comment.content}
                                      </p>

                                      {comment.mediaUrl && (
                                          <img
                                              src={comment.mediaUrl}
                                              alt="attachment"
                                              className="mt-2 rounded-xl max-h-48 object-cover border border-slate-200 dark:border-slate-700"
                                          />
                                      )}
                                  </div>

                                  {/* Actions under comment */}
                                  <div className="flex items-center gap-3 mt-1 ml-2 text-[11px] text-slate-400">
                                      <span>
                                          {new Date(
                                              comment.createdAt,
                                          ).toLocaleDateString("vi-VN")}
                                      </span>
                                      {!comment.deleted && (
                                          <button
                                              type="button"
                                              onClick={() =>
                                                  setReplyingToCommentId(
                                                      replyingToCommentId ===
                                                          comment.commentId
                                                          ? null
                                                          : comment.commentId,
                                                  )
                                              }
                                              className="font-medium hover:text-blue-500 cursor-pointer"
                                          >
                                              Trả lời
                                          </button>
                                      )}

                                      {user?.id === comment.author?.userId && (
                                          <button
                                              type="button"
                                              onClick={() =>
                                                  deleteComment(
                                                      comment.commentId,
                                                  )
                                              }
                                              className="hover:text-red-500 cursor-pointer opacity-0 group-hover/item:opacity-100 transition-opacity"
                                              title="Xóa bình luận"
                                          >
                                              <Trash2 className="w-3 h-3" />
                                          </button>
                                      )}
                                  </div>

                                  {/* Reply Input Box */}
                                  {replyingToCommentId ===
                                      comment.commentId && (
                                      <div className="flex gap-2 mt-2 ml-4 items-center animate-in fade-in duration-150">
                                          <CornerDownRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                                          <input
                                              type="text"
                                              value={replyContent}
                                              onChange={(e) =>
                                                  setReplyContent(
                                                      e.target.value,
                                                  )
                                              }
                                              placeholder={`Trả lời ${comment.author?.fullName}...`}
                                              onKeyDown={(e) => {
                                                  if (e.key === "Enter") {
                                                      e.preventDefault();
                                                      handleReplySubmit(
                                                          comment.commentId,
                                                      );
                                                  }
                                              }}
                                              className="flex-1 bg-slate-50 dark:bg-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:border-blue-500"
                                          />
                                          <button
                                              type="button"
                                              onClick={() =>
                                                  handleReplySubmit(
                                                      comment.commentId,
                                                  )
                                              }
                                              disabled={isCreating || !replyContent.trim()}
                                              className="p-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl cursor-pointer"
                                          >
                                              <Send className="w-3 h-3" />
                                          </button>
                                      </div>
                                  )}

                                  <CommentReplies key={comment.commentId} comment={comment}
                                    targetType={targetType} targetId={targetId} />
                              </div>
                          </div>
                      </div>
                  ))}

                  <InfiniteListFooter query={commentsQuery} label="Xem các bình luận trước đó" scrollRoot={commentsRoot} />
              </div>
          )}
      </section>
  );
};

function CommentReplies({ comment, targetType, targetId }: CommentSectionProps & { comment: CommentResponseDto }) {
  const [open, setOpen] = useState(false);
  const listRef = useRef<HTMLDivElement>(null);
  const query = useCommentReplies(targetType, targetId, comment.commentId, open);
  const previews = comment.replies ?? [];
  const replies = open && query.data ? query.data.pages.flatMap((page) => page.items) : previews;
  const total = comment.replyCount ?? previews.length;
  if (total === 0 && !open) return null;

  return (
    <div className="mt-2 ml-5 space-y-2 border-l-2 border-border pl-3">
      <div ref={listRef} className={open ? 'max-h-64 space-y-2 overflow-y-auto overscroll-contain' : 'space-y-2'}>
        {replies.map((reply) => (
          <div key={reply.commentId} className="flex items-start gap-2 text-xs">
            <UserAvatar avatarUrl={reply.author?.avatarUrl} name={reply.author?.fullName} decorative className="size-5" />
            <p className="whitespace-pre-wrap rounded-xl bg-muted px-2.5 py-1.5">
              <strong className="mr-1.5">{reply.author?.fullName}</strong>{reply.content}
            </p>
          </div>
        ))}
        {open && query.isPending && <p role="status" className="text-sm text-muted-foreground">Đang tải phản hồi…</p>}
        {open && query.isLoadingError && <Button type="button" variant="outline" onClick={() => void query.refetch()}>Thử tải lại phản hồi</Button>}
        {open && <InfiniteListFooter query={query} scrollRoot={listRef} label="Xem thêm phản hồi" />}
      </div>
      {(open || total > previews.length) && (
        <Button type="button" variant="ghost" aria-expanded={open} onClick={() => setOpen(!open)}>
          {open ? 'Thu gọn phản hồi' : `Xem tất cả ${total} phản hồi`}
        </Button>
      )}
    </div>
  );
}
