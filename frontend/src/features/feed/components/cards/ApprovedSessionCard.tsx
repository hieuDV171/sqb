import React from 'react';
import { useNavigate } from 'react-router-dom';
import type { ActivityFeedItemDto } from '../../types/feed.types';
import { CheckCircle2, BookOpen, ArrowRight, Sparkles } from 'lucide-react';

interface ApprovedSessionCardProps {
  item: ActivityFeedItemDto;
}

export const ApprovedSessionCard: React.FC<ApprovedSessionCardProps> = ({ item }) => {
  const navigate = useNavigate();
  const { actor, content, createdAt } = item;

  return (
    <div className="bg-gradient-to-br from-emerald-500/10 via-emerald-500/5 to-transparent dark:from-emerald-950/30 dark:via-slate-900 dark:to-slate-900 rounded-3xl border border-emerald-500/30 dark:border-emerald-800/50 p-5 shadow-xs transition-all hover:shadow-md mb-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-3">
          <img
            src={
              actor.avatarUrl ||
              'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
            }
            alt={actor.fullName}
            className="w-10 h-10 rounded-full object-cover ring-2 ring-emerald-500/40"
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
            {content.subject_code ? `[${content.subject_code}] ` : ''}
            {content.subject_name || 'Học phần Bách Khoa'}
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
      <div className="flex items-center justify-end">
        <button
          type="button"
          onClick={() => {
            if (content.sessionId) {
              navigate(`/sessions/${content.sessionId}`);
            } else {
              navigate('/sessions');
            }
          }}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-semibold text-xs shadow-md shadow-emerald-600/20 active:scale-95 transition-all cursor-pointer"
        >
          <span>Luyện tập ngay</span>
          <ArrowRight className="w-3.5 h-3.5" />
        </button>
      </div>
    </div>
  );
};
