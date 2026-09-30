import React, { useState } from 'react';
import type { CommentResponseDto } from '@/types/post.types';
import { useAuthStore } from '@/stores/useAuthStore';
import { Send, CornerDownRight } from 'lucide-react';

interface CommentSectionProps {
  postId: number;
  comments: CommentResponseDto[];
  onAddComment: (postId: number, content: string, parentCommentId?: number) => void;
}

export function CommentSection({ postId, comments, onAddComment }: CommentSectionProps) {
  const { user } = useAuthStore();
  const [commentText, setCommentText] = useState('');
  const [replyingToId, setReplyingToId] = useState<number | null>(null);
  const [replyText, setReplyText] = useState('');

  const handleSendRootComment = (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim()) return;
    onAddComment(postId, commentText.trim());
    setCommentText('');
  };

  const handleSendReply = (parentCommentId: number) => {
    if (!replyText.trim()) return;
    onAddComment(postId, replyText.trim(), parentCommentId);
    setReplyText('');
    setReplyingToId(null);
  };

  return (
    <div className="pt-3 border-t border-slate-100 dark:border-slate-800 space-y-4">
      {/* TODO: Hỗ trợ thanh chuyển đổi sắp xếp bình luận ('newest' / 'oldest') để phục vụ thảo luận bài học theo trình tự thời gian */}
      {/* Root Input Box */}
      <form onSubmit={handleSendRootComment} className="flex gap-2">
        <div className="w-8 h-8 rounded-full bg-linear-to-tr from-indigo-500 to-violet-600 flex items-center justify-center text-white font-bold text-xs shrink-0 overflow-hidden">
          {user?.avatarUrl ? (
            <img src={user.avatarUrl} alt={user.username} className="w-full h-full object-cover" />
          ) : (
            user?.username?.charAt(0).toUpperCase() || 'S'
          )}
        </div>

        <div className="flex-1 relative">
          <input
            type="text"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder="Viết bình luận học thuật hoặc chia sẻ ý kiến..."
            className="w-full text-xs sm:text-sm pl-3.5 pr-10 py-2 bg-slate-50 dark:bg-slate-800 hover:bg-slate-100/80 dark:hover:bg-slate-800/90 focus:bg-white dark:focus:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl outline-none focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500/10 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 transition-all"
          />
          <button
            type="submit"
            disabled={!commentText.trim()}
            className="absolute right-2 top-1/2 -translate-y-1/2 p-1 text-indigo-600 dark:text-indigo-400 hover:text-indigo-700 dark:hover:text-indigo-300 disabled:opacity-40 disabled:cursor-not-allowed transition-opacity cursor-pointer"
          >
            <Send className="w-4 h-4" />
          </button>
        </div>
      </form>

      {/* Comment List */}
      <div className="space-y-3 pt-1">
        {comments.map((comment) => (
          <div key={comment.commentId} className="space-y-2">
            {/* Main Comment Item */}
            <div className="flex gap-2.5 group">
              <div className="w-7 h-7 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-700 dark:text-slate-200 font-bold text-xs shrink-0 overflow-hidden mt-0.5">
                {comment.author.avatarUrl ? (
                  <img src={comment.author.avatarUrl} alt={comment.author.fullName} className="w-full h-full object-cover" />
                ) : (
                  comment.author.fullName.charAt(0)
                )}
              </div>

              <div className="flex-1">
                <div className="bg-slate-50 dark:bg-slate-800/80 hover:bg-slate-100/80 dark:hover:bg-slate-800 transition-colors p-3 rounded-2xl rounded-tl-xs border border-slate-100 dark:border-slate-700/60 max-w-2xl">
                  <div className="flex items-center gap-2 mb-1">
                    <span className="font-bold text-xs text-slate-800 dark:text-slate-200">{comment.author.fullName}</span>
                    {comment.author.role === 'LECTURER' && (
                      <span className="text-[9px] font-bold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-700 dark:text-emerald-300 px-1.5 py-0.2 rounded border border-emerald-200 dark:border-emerald-800/50">
                        Giảng viên
                      </span>
                    )}
                    <span className="text-[10px] text-slate-400 dark:text-slate-500">• {comment.createdAt}</span>
                  </div>
                  <p className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed">{comment.content}</p>
                </div>

                {/* Sub Action: Reply Button */}
                <div className="flex items-center gap-3 mt-1 ml-2 text-[11px] text-slate-500 dark:text-slate-400">
                  <button
                    type="button"
                    onClick={() => {
                      setReplyingToId(replyingToId === comment.commentId ? null : comment.commentId);
                      setReplyText('');
                    }}
                    className="font-semibold text-slate-600 dark:text-slate-400 hover:text-indigo-600 dark:hover:text-indigo-400 cursor-pointer"
                  >
                    Trả lời
                  </button>
                </div>

                {/* Nested Reply Input */}
                {replyingToId === comment.commentId && (
                  <div className="mt-2 pl-4 flex gap-2">
                    <input
                      type="text"
                      autoFocus
                      value={replyText}
                      onChange={(e) => setReplyText(e.target.value)}
                      placeholder={`Trả lời ${comment.author.fullName}...`}
                      className="flex-1 text-xs pl-3 pr-3 py-1.5 bg-white dark:bg-slate-800 border border-indigo-200 dark:border-indigo-800 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 rounded-xl outline-none focus:border-indigo-500"
                      onKeyDown={(e) => {
                        if (e.key === 'Enter') {
                          e.preventDefault();
                          handleSendReply(comment.commentId);
                        }
                      }}
                    />
                    <button
                      type="button"
                      onClick={() => handleSendReply(comment.commentId)}
                      disabled={!replyText.trim()}
                      className="px-3 py-1 bg-indigo-600 text-white rounded-xl text-xs font-bold disabled:opacity-40 cursor-pointer"
                    >
                      Gửi
                    </button>
                  </div>
                )}

                {/* Nested Replies List */}
                {comment.replies && comment.replies.length > 0 && (
                  <div className="mt-2.5 space-y-2 pl-3 border-l-2 border-indigo-100 dark:border-indigo-900/50">
                    {comment.replies.map((reply) => (
                      <div key={reply.commentId} className="flex gap-2">
                        <CornerDownRight className="w-3.5 h-3.5 text-slate-400 dark:text-slate-500 mt-1 shrink-0" />
                        <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 font-bold text-[10px] shrink-0 overflow-hidden">
                          {reply.author.avatarUrl ? (
                            <img src={reply.author.avatarUrl} alt={reply.author.fullName} className="w-full h-full object-cover" />
                          ) : (
                            reply.author.fullName.charAt(0)
                          )}
                        </div>
                        <div className="bg-slate-50 dark:bg-slate-800/90 p-2.5 rounded-xl border border-slate-100 dark:border-slate-700/60 flex-1">
                          <div className="flex items-center gap-1.5 mb-0.5">
                            <span className="font-bold text-[11px] text-slate-800 dark:text-slate-200">{reply.author.fullName}</span>
                            <span className="text-[10px] text-slate-400 dark:text-slate-500">• {reply.createdAt}</span>
                          </div>
                          <p className="text-xs text-slate-700 dark:text-slate-300">{reply.content}</p>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
