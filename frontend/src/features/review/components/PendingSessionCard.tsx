import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { PendingSessionItemDto } from '../types/review.types';
import { BookOpen, Calendar, ArrowRight, UserCheck } from 'lucide-react';

interface PendingSessionCardProps {
  session: PendingSessionItemDto;
}

export const PendingSessionCard: React.FC<PendingSessionCardProps> = ({ session }) => {
  const navigate = useNavigate();

  const formattedDate = new Date(session.createdAt).toLocaleDateString('vi-VN', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  });

  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/90 dark:border-slate-800 p-5 md:p-6 shadow-xs hover:shadow-md transition-all space-y-4">
      {/* Header Info */}
      <div className="flex items-center justify-between flex-wrap gap-2">
        <div className="flex items-center gap-2">
          {session.topic && (
            <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-300 border border-indigo-200 dark:border-indigo-800 font-bold text-xs">
              <BookOpen className="w-3.5 h-3.5" />
              <span>{session.topic}</span>
            </span>
          )}

          <span className="text-xs font-mono font-semibold text-slate-400">
            #{session.sessionId}
          </span>
        </div>

        {/* Status Badge */}
        <span
          className={`px-2.5 py-1 rounded-full text-xs font-bold border ${
            session.status === 'REVIEWING'
              ? 'bg-blue-50 dark:bg-blue-950/60 text-blue-700 dark:text-blue-300 border-blue-200 dark:border-blue-800'
              : 'bg-amber-50 dark:bg-amber-950/60 text-amber-700 dark:text-amber-300 border-amber-200 dark:border-amber-800'
          }`}
        >
          {session.status === 'REVIEWING' ? '⏳ Đang xem xét' : '📥 Chờ duyệt'}
        </span>
      </div>

      {/* Title & Content */}
      <div className="space-y-1.5">
        <h4 className="font-bold text-base md:text-lg text-slate-800 dark:text-slate-100 hover:text-indigo-600 transition-colors">
          {session.title || 'Đề xuất câu hỏi ôn tập'}
        </h4>
        {session.content && (
          <p className="text-xs md:text-sm text-slate-600 dark:text-slate-400 line-clamp-2 leading-relaxed">
            {session.content}
          </p>
        )}
      </div>

      {/* Footer Info */}
      <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800/80 flex-wrap gap-3">
        {/* Author & Time */}
        <div className="flex items-center gap-3 text-xs text-slate-500 dark:text-slate-400">
          <div className="flex items-center gap-1.5">
            <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 flex items-center justify-center text-slate-600 dark:text-slate-300 text-[10px] font-bold overflow-hidden">
              {session.author.avatarUrl ? (
                <img
                  src={session.author.avatarUrl}
                  alt={session.author.fullName}
                  className="w-full h-full object-cover"
                />
              ) : (
                <UserCheck className="w-3.5 h-3.5" />
              )}
            </div>
            <span className="font-medium text-slate-700 dark:text-slate-300">
              {session.author.fullName}
            </span>
          </div>

          <div className="flex items-center gap-1 text-[11px]">
            <Calendar className="w-3.5 h-3.5 text-slate-400" />
            <span>{formattedDate}</span>
          </div>
        </div>

        {/* Action Button */}
        <button
          type="button"
          onClick={() => navigate(`/lecturer/reviews/${session.sessionId}`)}
          className="flex items-center gap-1.5 px-4 py-2 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white font-semibold text-xs shadow-md shadow-indigo-500/20 active:scale-95 transition-all cursor-pointer"
        >
          <span>Đánh giá ngay</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
