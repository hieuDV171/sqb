import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Calendar,
  Heart,
  MessageSquare,
  Copy,
  Check,
  PlayCircle,
  HelpCircle,
} from 'lucide-react';
import type { SubmissionSessionSummaryDto } from '@/features/session/types/session.types';

interface UserSessionCardProps {
  session: SubmissionSessionSummaryDto;
}

export const UserSessionCard: React.FC<UserSessionCardProps> = ({ session }) => {
  const navigate = useNavigate();
  const [copied, setCopied] = useState(false);

  const handleCopyCode = (e: React.MouseEvent) => {
    e.stopPropagation();
    if (!session.sessionCode) return;
    navigator.clipboard.writeText(session.sessionCode);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  };

  const handleStartPractice = (e: React.MouseEvent) => {
    e.stopPropagation();
    navigate(`/sessions/${session.sessionId}/practice`);
  };

  return (
    <div
      onClick={() => navigate(`/sessions/${session.sessionId}`)}
      className="p-5 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200/80 dark:border-slate-800 hover:border-indigo-400 dark:hover:border-indigo-600 shadow-xs hover:shadow-md transition-all cursor-pointer space-y-4 group"
    >
      {/* Top Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="px-2.5 py-0.5 rounded-lg text-xs font-black bg-indigo-50 dark:bg-indigo-950/80 text-indigo-700 dark:text-indigo-300 border border-indigo-200/60 dark:border-indigo-800/60">
              {session.subjectCode || 'MÔN HỌC'}
            </span>

            {session.sessionCode && (
              <button
                type="button"
                onClick={handleCopyCode}
                className="inline-flex items-center gap-1 px-2 py-0.5 rounded-md text-[11px] font-mono text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors"
                title="Sao chép mã phiên"
              >
                <span>#{session.sessionCode}</span>
                {copied ? (
                  <Check className="w-3 h-3 text-emerald-500" />
                ) : (
                  <Copy className="w-3 h-3" />
                )}
              </button>
            )}
          </div>

          <h3 className="font-bold text-slate-900 dark:text-white text-sm sm:text-base group-hover:text-indigo-600 transition-colors line-clamp-1">
            {session.title}
          </h3>
        </div>

        {/* Action Button: Start Practice */}
        <button
          type="button"
          onClick={handleStartPractice}
          className="flex items-center gap-1.5 px-3.5 py-2 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold transition-all shadow-xs active:scale-95 cursor-pointer shrink-0"
        >
          <PlayCircle className="w-4 h-4" />
          <span>Làm bài</span>
        </button>
      </div>

      {/* Description / Content snippet */}
      {session.content && (
        <p className="text-xs text-slate-500 dark:text-slate-400 line-clamp-2 leading-relaxed">
          {session.content}
        </p>
      )}

      {/* Footer Info */}
      <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800 text-xs text-slate-500 dark:text-slate-400">
        <div className="flex items-center gap-4">
          <span className="flex items-center gap-1 font-semibold text-slate-700 dark:text-slate-300">
            <HelpCircle className="w-3.5 h-3.5 text-indigo-500" />
            {session.questionCounts || 0} câu hỏi
          </span>

          <span className="flex items-center gap-1">
            <Heart className="w-3.5 h-3.5 text-rose-500" />
            {session.reactCount || 0}
          </span>

          <span className="flex items-center gap-1">
            <MessageSquare className="w-3.5 h-3.5 text-blue-500" />
            {session.commentCount || 0}
          </span>
        </div>

        <div className="flex items-center gap-1 text-[11px] text-slate-400">
          <Calendar className="w-3 h-3" />
          {new Date(session.createdAt).toLocaleDateString('vi-VN')}
        </div>
      </div>
    </div>
  );
};
