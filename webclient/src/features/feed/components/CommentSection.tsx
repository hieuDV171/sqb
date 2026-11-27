import { InfiniteListFooter } from '@/components/ui/infinite-list-footer';
import type { CommentResponseDto } from '@/types/post.types';
import React, { useRef, useState } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useComments, useCommentReplies } from '../hooks/useComments';
import { mediaService } from '@/services/mediaService';
import { ActionError } from '@/components/ui/action-error';
import { getActionErrorMessage } from '@/lib/errorMessage';
import { Button } from '@/components/ui/button';
import { Skeleton } from '@/components/ui/skeleton';
import { UserAvatar } from '@/components/ui/user-avatar';
import { EditComment } from './EditComment';
import { AuthorProfileLink } from './AuthorProfileLink';
import { ReportCommentAction } from '@/features/report/components/ReportCommentAction';
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
    deleteError,
    failedDeleteId,
    commentsQuery,
  } = useComments(targetType, targetId);

  const commentsRoot = useRef<HTMLDivElement>(null);
  const [commentError, setCommentError] = useState<string>();
  const [replyError, setReplyError] = useState<{ id: number; message: string }>();
  const [inputContent, setInputContent] = useState('');
  const [replyingToCommentId, setReplyingToCommentId] = useState<number | null>(null);
  const [replyContent, setReplyContent] = useState('');
  const [commentMediaUrl, setCommentMediaUrl] = useState<string | null>(null);
  const [isUploadingMedia, setIsUploadingMedia] = useState(false);

  const handleMediaUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;
    setCommentError(undefined);
    setIsUploadingMedia(true);
    try {
      const { publicUrl } = await mediaService.uploadViaPresign(file, 'POST');
      setCommentMediaUrl(publicUrl);
    } catch (error) {
      setCommentError(getActionErrorMessage(error, 'Không thể tải ảnh bình luận lên.'));
    } finally {
      setIsUploadingMedia(false);
    }
  };

  const handleSubmitComment = async (e: React.SubmitEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (isCreating || (!inputContent.trim() && !commentMediaUrl)) return;

    try {
      setCommentError(undefined);
      await createComment({ content: inputContent.trim(), mediaUrl: commentMediaUrl || undefined });
      setInputContent('');
      setCommentMediaUrl(null);
    } catch (failure) {
      setCommentError(getActionErrorMessage(failure, 'Không thể đăng bình luận. Hãy thử lại.'));
    }
  };

  const handleReplySubmit = async (parentCommentId: number) => {
    if (isCreating || !replyContent.trim()) return;

    try {
      setReplyError(undefined);
      await createComment({ content: replyContent.trim(), parentCommentId });
      setReplyContent('');
      setReplyingToCommentId(null);
    } catch (failure) {
      setReplyError({ id: parentCommentId, message: getActionErrorMessage(failure, 'Không thể gửi phản hồi. Hãy thử lại.') });
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
              <AuthorProfileLink userId={user?.id} name={avatarName} className="grid size-11 shrink-0 place-items-center rounded-full">
                <UserAvatar
                  avatarUrl={user?.avatarUrl}
                  name={avatarName}
                  className="size-10"
                  decorative
                />
              </AuthorProfileLink>

              <div className="min-w-0 flex-1 rounded-2xl border border-border bg-surface-subtle p-2.5 transition-[border-color,box-shadow] focus-within:border-primary/40 focus-within:ring-2 focus-within:ring-ring/30">
                  <textarea
                      aria-label="Nội dung bình luận"
                      value={inputContent}
                      onChange={(e) => setInputContent(e.target.value)}
                      placeholder={targetType === 'SESSION' ? 'Chia sẻ góp ý về cả bộ câu hỏi…' : 'Viết bình luận hoặc lời giải của bạn...'}
                      rows={2}
                      className="w-full resize-none bg-transparent px-1 py-1 text-base leading-relaxed text-foreground placeholder:text-muted-foreground focus:outline-none"
                  />

                  {commentMediaUrl && (
                      <div className="relative inline-block mt-2">
                          <img
                              src={commentMediaUrl}
                              alt="Ảnh đính kèm bình luận"
                              className="size-20 rounded-xl border border-border object-cover"
                          />
                          <button
                              type="button"
                              aria-label="Gỡ ảnh đính kèm"
                              onClick={() => setCommentMediaUrl(null)}
                              className="absolute -top-1.5 -right-1.5 p-1 bg-black/70 hover:bg-black text-white rounded-full"
                          >
                              <X className="w-3 h-3" />
                          </button>
                      </div>
                  )}

                  <div className="mt-1 flex items-center justify-between border-t border-border pt-1.5">
                      <label className="grid size-11 cursor-pointer place-items-center rounded-xl text-muted-foreground transition-colors hover:bg-muted hover:text-primary focus-within:ring-2 focus-within:ring-ring">
                          <ImageIcon className="w-4 h-4" />
                          <input
                              type="file"
                              accept="image/*"
                              onChange={handleMediaUpload}
                              className="sr-only"
                              aria-label="Đính kèm ảnh"
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

          <ActionError message={commentError} className="mb-4" />
          {/* 2. Comments List */}
          {isLoading ? (
              <div className="space-y-4 py-2" role="status">
                  <span className="sr-only">Đang tải bình luận</span>
                  {[0, 1].map((item) => (
                      <div key={item} className="flex gap-2.5" aria-hidden="true">
                          <Skeleton className="size-10 shrink-0 rounded-full" />
                          <Skeleton className="h-20 w-full rounded-2xl" />
                      </div>
                  ))}
              </div>
          ) : comments.length === 0 ? (
              <div className="py-5 text-center text-sm text-muted-foreground">
                  Chưa có bình luận nào. Hãy là người đầu tiên trao đổi!
              </div>
          ) : (
              // Allow wheel/touch scrolling to continue into the page at either boundary.
              <div ref={commentsRoot} className="max-h-[min(65dvh,36rem)] space-y-4 overflow-y-auto overscroll-auto pr-1">
                  {comments.map((comment) => (
                      <div key={comment.commentId} className="group/item">
                          <div className="flex gap-2.5 items-start">
                              <AuthorProfileLink userId={comment.author?.userId} name={comment.author?.fullName} className="grid size-11 shrink-0 place-items-center rounded-full">
                                <UserAvatar
                                  avatarUrl={comment.author?.avatarUrl}
                                  name={comment.author?.fullName}
                                  className="size-10"
                                  decorative
                                />
                              </AuthorProfileLink>

                              <div className="min-w-0 flex-1">
                                  <div className="rounded-2xl border border-border bg-surface-subtle px-3.5 py-2.5 text-base leading-relaxed text-foreground">
                                      <div className="mb-1 flex flex-wrap items-center gap-x-2 gap-y-1">
                                          <AuthorProfileLink userId={comment.author?.userId} name={comment.author?.fullName} className="min-w-0 wrap-anywhere text-sm font-semibold hover:underline">
                                              {comment.author?.fullName ||
                                                  "Người dùng SQB"}
                                          </AuthorProfileLink>
                                          {comment.author?.role && (
                                              <span
                                                  className={`rounded-full px-2 py-0.5 text-xs font-medium ${
                                                      comment.author.role ===
                                                      "LECTURER"
                                                          ? "bg-academic-soft text-academic"
                                                          : "bg-primary/10 text-primary"
                                                  }`}
                                              >
                                                  {comment.author.role ===
                                                  "LECTURER"
                                                      ? "Giảng viên"
                                                      : comment.author.role === 'ADMIN' ? 'Quản trị viên' : "Sinh viên"}
                                              </span>
                                          )}
                                      </div>
                                      <p className="wrap-anywhere whitespace-pre-wrap">
                                          {comment.content}
                                      </p>

                                      {comment.mediaUrl && (
                                          <img
                                              src={comment.mediaUrl}
                                              alt="Ảnh đính kèm bình luận"
                                              className="mt-2 max-h-48 max-w-full rounded-xl border border-border object-contain"
                                          />
                                      )}
                                  </div>

                                  {/* Actions under comment */}
                                  <div className="ml-1 flex flex-wrap items-center gap-x-3 text-sm text-muted-foreground">
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
                                              aria-expanded={replyingToCommentId === comment.commentId}
                                              className="min-h-11 px-1 font-medium transition-colors hover:text-primary"
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
                                              className="grid size-11 place-items-center rounded-xl transition-colors hover:bg-destructive/10 hover:text-destructive"
                                              aria-label="Xóa bình luận"
                                              title="Xóa bình luận"
                                          >
                                              <Trash2 className="size-4" aria-hidden="true" />
                                          </button>
                                      )}
                                  </div>

                                  <EditComment comment={comment} />
                                  <ReportCommentAction comment={comment} />

                                  {failedDeleteId === comment.commentId && (
                                    <ActionError message={getActionErrorMessage(deleteError, 'Không thể xóa bình luận. Hãy thử lại.')} />
                                  )}
                                  {/* Reply Input Box */}
                                  {replyingToCommentId ===
                                      comment.commentId && (
                                      <div className="mt-2 flex items-center gap-2">
                                          <CornerDownRight className="hidden size-4 shrink-0 text-muted-foreground sm:block" aria-hidden="true" />
                                          <input
                                              type="text"
                                              aria-label={`Trả lời ${comment.author?.fullName || 'người dùng SQB'}`}
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
                                              className="min-h-11 min-w-0 flex-1 rounded-xl border border-input bg-surface-subtle px-3 py-2 text-base text-foreground placeholder:text-muted-foreground focus-visible:border-primary"
                                          />
                                          <button
                                              type="button"
                                              onClick={() =>
                                                  handleReplySubmit(
                                                      comment.commentId,
                                                  )
                                              }
                                              disabled={isCreating || !replyContent.trim()}
                                              aria-label="Gửi phản hồi"
                                              className="grid size-11 shrink-0 place-items-center rounded-xl bg-primary text-primary-foreground transition-colors hover:bg-primary/85 disabled:opacity-40"
                                          >
                                              <Send className="size-4" aria-hidden="true" />
                                          </button>
                                      </div>
                                  )}

                                  {replyingToCommentId === comment.commentId && replyError?.id === comment.commentId && (
                                    <ActionError message={replyError.message} />
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
    <div className="mt-2 space-y-3 border-l-2 border-border pl-2 sm:pl-3">
      <div ref={listRef} className={open ? 'max-h-64 space-y-3 overflow-y-auto overscroll-auto pr-1' : 'space-y-3'}>
        {replies.map((reply) => (
          <div key={reply.commentId} className="flex items-start gap-2">
            <AuthorProfileLink userId={reply.author?.userId} name={reply.author?.fullName} className="grid size-11 shrink-0 place-items-center rounded-full">
              <UserAvatar avatarUrl={reply.author?.avatarUrl} name={reply.author?.fullName} decorative className="size-9" />
            </AuthorProfileLink>
            <div className="min-w-0 flex-1">
              <div className="rounded-2xl border border-border bg-surface-subtle px-3 py-2.5 text-foreground">
                <AuthorProfileLink userId={reply.author?.userId} name={reply.author?.fullName} className="wrap-anywhere text-sm font-semibold hover:underline">
                  {reply.author?.fullName || 'Người dùng SQB'}
                </AuthorProfileLink>
                <p className="mt-1 wrap-anywhere whitespace-pre-wrap text-base leading-relaxed">{reply.content}</p>
              </div>
              <EditComment comment={reply} />
              <ReportCommentAction comment={reply} />
            </div>
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
