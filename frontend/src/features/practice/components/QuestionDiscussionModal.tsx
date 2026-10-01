import React, { useState } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { X, MessageSquare, Send, Loader2, User } from 'lucide-react';
import { commentService } from '@/features/feed/services/commentService';
import type { CommentResponseDto } from '@/types/post.types';

interface QuestionDiscussionModalProps {
  questionId: number | null;
  questionCode?: string;
  isOpen: boolean;
  onClose: () => void;
}

export const QuestionDiscussionModal: React.FC<QuestionDiscussionModalProps> = ({
  questionId,
  questionCode,
  isOpen,
  onClose,
}) => {
  const queryClient = useQueryClient();
  const [commentText, setCommentText] = useState('');

  const {
    data: commentsData,
    isLoading,
  } = useQuery({
    queryKey: ['questionComments', questionId],
    queryFn: async () => {
      if (!questionId) return [];
      const res = await commentService.getComments({
        targetType: 'QUESTION',
        targetId: questionId,
        limit: 50,
      });
      return (res?.data?.items || []) as CommentResponseDto[];
    },
    enabled: !!questionId && isOpen,
  });

  const { mutateAsync: sendComment, isPending: isSending } = useMutation({
    mutationFn: async (text: string) => {
      if (!questionId) return;
      await commentService.createComment({
        targetType: 'QUESTION',
        targetId: questionId,
        content: text,
      });
    },
    onSuccess: () => {
      setCommentText('');
      queryClient.invalidateQueries({ queryKey: ['questionComments', questionId] });
    },
  });

  if (!isOpen || !questionId) return null;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!commentText.trim() || isSending) return;
    await sendComment(commentText.trim());
  };

  const comments = commentsData || [];

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-950/60 backdrop-blur-xs animate-in fade-in duration-200">
      <div className="relative w-full max-w-lg rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl flex flex-col max-h-[85vh] overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between p-5 border-b border-slate-100 dark:border-slate-800 shrink-0">
          <div className="flex items-center gap-2.5">
            <span className="p-2 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <MessageSquare className="w-5 h-5" />
            </span>
            <div>
              <h3 className="font-bold text-slate-900 dark:text-white text-base">
                Thảo luận về câu hỏi
              </h3>
              {questionCode && (
                <p className="text-xs text-slate-400 font-mono">Mã: {questionCode}</p>
              )}
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Comment list */}
        <div className="flex-1 overflow-y-auto p-5 space-y-4">
          {isLoading ? (
            <div className="py-12 flex flex-col items-center justify-center space-y-2">
              <Loader2 className="w-6 h-6 animate-spin text-indigo-500" />
              <p className="text-xs text-slate-400">Đang tải các bình luận...</p>
            </div>
          ) : comments.length === 0 ? (
            <div className="py-12 text-center space-y-2">
              <div className="w-12 h-12 rounded-2xl bg-slate-100 dark:bg-slate-800 flex items-center justify-center mx-auto text-slate-400">
                <MessageSquare className="w-6 h-6" />
              </div>
              <p className="text-xs font-semibold text-slate-600 dark:text-slate-400">
                Chưa có trao đổi nào
              </p>
              <p className="text-[11px] text-slate-400 max-w-xs mx-auto">
                Hãy là người đầu tiên để lại thắc mắc, phân tích hoặc mẹo giải cho câu hỏi này!
              </p>
            </div>
          ) : (
            comments.map((c) => (
              <div key={c.commentId} className="flex gap-3 text-xs">
                {c.author?.avatarUrl ? (
                  <img
                    src={c.author.avatarUrl}
                    alt={c.author.fullName || 'User'}
                    className="w-8 h-8 rounded-full object-cover shrink-0 ring-1 ring-slate-200 dark:ring-slate-700"
                  />
                ) : (
                  <div className="w-8 h-8 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-500 shrink-0">
                    <User className="w-4 h-4" />
                  </div>
                )}
                <div className="flex-1 space-y-1">
                  <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200/60 dark:border-slate-700/60 space-y-1">
                    <div className="flex items-center justify-between">
                      <span className="font-bold text-slate-900 dark:text-white">
                        {c.author?.fullName || 'Thành viên'}
                      </span>
                      <span className="text-[10px] text-slate-400">
                        {new Date(c.createdAt).toLocaleTimeString('vi-VN', {
                          hour: '2-digit',
                          minute: '2-digit',
                        })}{' '}
                        • {new Date(c.createdAt).toLocaleDateString('vi-VN')}
                      </span>
                    </div>
                    <p className="text-slate-700 dark:text-slate-300 leading-relaxed break-words">
                      {c.content}
                    </p>
                  </div>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Input box */}
        <form
          onSubmit={handleSubmit}
          className="p-3.5 border-t border-slate-100 dark:border-slate-800 bg-slate-50/50 dark:bg-slate-900/50 flex items-center gap-2"
        >
          <input
            type="text"
            value={commentText}
            onChange={(e) => setCommentText(e.target.value)}
            placeholder="Viết thắc mắc hoặc lời giải thích của bạn..."
            className="flex-1 bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl px-3.5 py-2.5 text-xs text-slate-900 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden focus:border-indigo-500 transition-all"
          />
          <button
            type="submit"
            disabled={!commentText.trim() || isSending}
            className="p-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 disabled:opacity-40 text-white transition-all shadow-xs cursor-pointer active:scale-95 shrink-0"
          >
            {isSending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </button>
        </form>
      </div>
    </div>
  );
};
