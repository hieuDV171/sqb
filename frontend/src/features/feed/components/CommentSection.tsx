import React, { useState } from 'react';
import { useAuthStore } from '@/stores/useAuthStore';
import { useComments } from '../hooks/useComments';
import { mediaService } from '@/services/mediaService';
import {
  Send,
  Loader2,
  Trash2,
  CornerDownRight,
  Image as ImageIcon,
  X,
} from 'lucide-react';

interface CommentSectionProps {
  targetType: 'POST' | 'COMMENT';
  targetId: number;
}

export const CommentSection: React.FC<CommentSectionProps> = ({ targetType, targetId }) => {
  const { user } = useAuthStore();
  const {
    comments,
    isLoading,
    isCreating,
    createComment,
    deleteComment,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
  } = useComments(targetType, targetId);

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
    if (!inputContent.trim() && !commentMediaUrl) return;

    await createComment({
      content: inputContent.trim(),
      mediaUrl: commentMediaUrl || undefined,
    });

    setInputContent('');
    setCommentMediaUrl(null);
  };

  const handleReplySubmit = async (parentCommentId: number) => {
    if (!replyContent.trim()) return;

    await createComment({
      content: replyContent.trim(),
      parentCommentId,
    });

    setReplyContent('');
    setReplyingToCommentId(null);
  };

  return (
    <div className="pt-3 mt-3 border-t border-slate-100 dark:border-slate-800">
      {/* 1. Comment Input Box */}
      <form onSubmit={handleSubmitComment} className="flex gap-2.5 items-start mb-4">
        <img
          src={
            user?.avatarUrl ||
            'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
          }
          alt={user?.username || 'User'}
          className="w-8 h-8 rounded-full object-cover shrink-0 mt-0.5"
        />

        <div className="flex-1 bg-slate-50 dark:bg-slate-800/80 rounded-2xl p-2 border border-slate-200 dark:border-slate-700 focus-within:ring-2 focus-within:ring-blue-500/20 focus-within:border-blue-500/40 transition-all">
          <textarea
            value={inputContent}
            onChange={(e) => setInputContent(e.target.value)}
            placeholder="Viết bình luận hoặc lời giải của bạn..."
            rows={1}
            className="w-full bg-transparent text-xs md:text-sm text-slate-800 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden resize-none px-1"
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
            <label className="p-1.5 rounded-lg text-slate-400 hover:text-blue-600 hover:bg-blue-50 dark:hover:bg-slate-700 cursor-pointer transition-colors">
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
              disabled={isCreating || isUploadingMedia || (!inputContent.trim() && !commentMediaUrl)}
              className="flex items-center gap-1.5 px-3 py-1 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-40 text-white text-xs font-medium cursor-pointer shadow-xs active:scale-95 transition-all"
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
        <div className="flex items-center justify-center py-6">
          <Loader2 className="w-5 h-5 animate-spin text-blue-500" />
        </div>
      ) : comments.length === 0 ? (
        <div className="text-center py-4 text-xs text-slate-400">
          Chưa có bình luận nào. Hãy là người đầu tiên trao đổi!
        </div>
      ) : (
        <div className="space-y-3">
          {comments.map((comment) => (
            <div key={comment.commentId} className="group/item">
              <div className="flex gap-2.5 items-start">
                <img
                  src={
                    comment.author?.avatarUrl ||
                    'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
                  }
                  alt={comment.author?.fullName || 'Author'}
                  className="w-7 h-7 rounded-full object-cover shrink-0 mt-0.5"
                />

                <div className="flex-1">
                  <div className="inline-block bg-slate-100 dark:bg-slate-800/80 rounded-2xl px-3 py-2 text-xs text-slate-800 dark:text-slate-200 border border-slate-200/60 dark:border-slate-700/60">
                    <div className="flex items-center gap-2 mb-0.5">
                      <span className="font-semibold text-slate-900 dark:text-white">
                        {comment.author?.fullName || 'Người dùng SQB'}
                      </span>
                      {comment.author?.role && (
                        <span
                          className={`text-[9px] font-bold px-1.5 py-0.2 rounded-full ${
                            comment.author.role === 'LECTURER'
                              ? 'bg-amber-100 dark:bg-amber-900/50 text-amber-700 dark:text-amber-400'
                              : 'bg-blue-100 dark:bg-blue-900/50 text-blue-700 dark:text-blue-400'
                          }`}
                        >
                          {comment.author.role === 'LECTURER' ? 'Giảng viên' : 'Sinh viên'}
                        </span>
                      )}
                    </div>
                    <p className="whitespace-pre-wrap">{comment.content}</p>

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
                    <span>{new Date(comment.createdAt).toLocaleDateString('vi-VN')}</span>
                    <button
                      type="button"
                      onClick={() =>
                        setReplyingToCommentId(
                          replyingToCommentId === comment.commentId ? null : comment.commentId
                        )
                      }
                      className="font-medium hover:text-blue-500 cursor-pointer"
                    >
                      Trả lời
                    </button>

                    {user?.id === comment.author?.userId && (
                      <button
                        type="button"
                        onClick={() => deleteComment(comment.commentId)}
                        className="hover:text-red-500 cursor-pointer opacity-0 group-hover/item:opacity-100 transition-opacity"
                        title="Xóa bình luận"
                      >
                        <Trash2 className="w-3 h-3" />
                      </button>
                    )}
                  </div>

                  {/* Reply Input Box */}
                  {replyingToCommentId === comment.commentId && (
                    <div className="flex gap-2 mt-2 ml-4 items-center animate-in fade-in duration-150">
                      <CornerDownRight className="w-3.5 h-3.5 text-slate-400 shrink-0" />
                      <input
                        type="text"
                        value={replyContent}
                        onChange={(e) => setReplyContent(e.target.value)}
                        placeholder={`Trả lời ${comment.author?.fullName}...`}
                        onKeyDown={(e) => {
                          if (e.key === 'Enter') {
                            e.preventDefault();
                            handleReplySubmit(comment.commentId);
                          }
                        }}
                        className="flex-1 bg-slate-50 dark:bg-slate-800 rounded-xl px-3 py-1.5 text-xs text-slate-800 dark:text-slate-100 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:border-blue-500"
                      />
                      <button
                        type="button"
                        onClick={() => handleReplySubmit(comment.commentId)}
                        disabled={!replyContent.trim()}
                        className="p-1.5 bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white rounded-xl cursor-pointer"
                      >
                        <Send className="w-3 h-3" />
                      </button>
                    </div>
                  )}

                  {/* Nested Replies Rendering */}
                  {comment.replies && comment.replies.length > 0 && (
                    <div className="mt-2.5 ml-5 space-y-2 border-l-2 border-slate-200 dark:border-slate-700 pl-3">
                      {comment.replies.map((reply) => (
                        <div key={reply.commentId} className="flex gap-2 items-start">
                          <img
                            src={
                              reply.author?.avatarUrl ||
                              'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
                            }
                            alt={reply.author?.fullName || 'User'}
                            className="w-5 h-5 rounded-full object-cover shrink-0 mt-0.5"
                          />
                          <div className="bg-slate-50 dark:bg-slate-800/60 rounded-xl px-2.5 py-1.5 text-xs text-slate-800 dark:text-slate-200">
                            <span className="font-semibold text-slate-900 dark:text-white mr-1.5">
                              {reply.author?.fullName}
                            </span>
                            <span>{reply.content}</span>
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </div>
              </div>
            </div>
          ))}

          {hasNextPage && (
            <button
              type="button"
              onClick={() => fetchNextPage()}
              disabled={isFetchingNextPage}
              className="w-full py-1.5 text-center text-xs font-semibold text-blue-600 dark:text-blue-400 hover:underline cursor-pointer"
            >
              {isFetchingNextPage ? 'Đang tải thêm...' : 'Xem các bình luận trước đó'}
            </button>
          )}
        </div>
      )}
    </div>
  );
};
