import React from 'react';
import { Link } from 'react-router-dom';
import type { ActivityFeedItemDto } from '../../types/feed.types';
import { UserAvatar } from '@/components/ui/user-avatar';
import { CheckCircle2, BookOpen, ArrowRight, Sparkles } from 'lucide-react';
import { PostFeedCard } from '../PostFeedCard';

interface ApprovedSessionCardProps {
  item: ActivityFeedItemDto;
}

export const ApprovedSessionCard: React.FC<ApprovedSessionCardProps> = ({ item }) => {
  if (item.targetType === 'POST') return <PostFeedCard item={item} />;

  const { actor, content, createdAt } = item;
  const sessionId = item.targetType === 'SESSION' ? item.targetId ?? content.sessionId : content.sessionId;

  return (
    <div className="bg-gradient-to-br from-emerald-500/10 via-emerald-500/5 to-transparent dark:from-emerald-950/30 dark:via-slate-900 dark:to-slate-900 rounded-3xl border border-emerald-500/30 dark:border-emerald-800/50 p-5 shadow-xs transition-all hover:shadow-md mb-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-3">
          <UserAvatar
            avatarUrl={actor.avatarUrl}
            name={actor.fullName}
            className="ring-2 ring-emerald-500/40"
            decorative
          />
          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-bold text-slate-900 dark:text-white text-sm">
                {actor.fullName}
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                vừa đóng góp thành công
              </span>
            </div>
            <p className="text-[11px] text-slate-400">
              {new Date(createdAt).toLocaleDateString('vi-VN', {
                hour: '2-digit',
                minute: '2-digit',
                day: '2-digit',
                month: '2-digit',
              })}
            </p>
          </div>
        </div>

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-emerald-100 dark:bg-emerald-900/60 text-emerald-700 dark:text-emerald-300 font-semibold text-xs border border-emerald-300 dark:border-emerald-700">
          <CheckCircle2 className="w-3.5 h-3.5" />
          <span>Đã duyệt vào Ngân hàng</span>
        </div>
      </div>

      {/* Body Content */}
      <div className="bg-white/80 dark:bg-slate-800/80 backdrop-blur-xs rounded-2xl p-4 border border-emerald-500/20 dark:border-emerald-900/50 mb-3.5">
        <div className="flex items-center gap-2 mb-1.5">
          <BookOpen className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
          <span className="font-semibold text-xs text-emerald-700 dark:text-emerald-300">
            {content.subjectCode ? `[${content.subjectCode}] ` : ''}
            {content.subjectName || 'Học phần Bách Khoa'}
          </span>
        </div>

        <h4 className="font-bold text-slate-900 dark:text-white text-base mb-1">
          {content.title || 'Bộ câu hỏi trắc nghiệm mới được kiểm duyệt'}
        </h4>

        {content.description && (
          <p className="text-xs text-slate-600 dark:text-slate-300 mb-3 line-clamp-2">
            {content.description}
          </p>
        )}

        {content.pointsEarned && (
          <div className="flex items-center gap-1.5 text-xs font-semibold text-amber-600 dark:text-amber-400 bg-amber-50 dark:bg-amber-950/40 px-2.5 py-1 rounded-xl w-fit">
            <Sparkles className="w-3.5 h-3.5" />
            <span>+{content.pointsEarned} Điểm Gamification đóng góp</span>
          </div>
        )}
      </div>

      {/* Action */}
      <div className="flex flex-wrap items-center justify-between gap-3">
        {typeof sessionId === 'number' && Number.isSafeInteger(sessionId) && sessionId > 0 && <Link
          to={`/sessions/${sessionId}/practice`}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs shadow-md shadow-emerald-600/20 active:scale-95 transition-all cursor-pointer"
        >
          <span>Luyện tập ngay</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </Link>}
      </div>
    </div>
  );
};
