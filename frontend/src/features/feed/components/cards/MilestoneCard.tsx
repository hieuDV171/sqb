import React from 'react';
import type { ActivityFeedItemDto } from '../../types/feed.types';
import { Flame, Trophy } from 'lucide-react';

interface MilestoneCardProps {
  item: ActivityFeedItemDto;
}

export const MilestoneCard: React.FC<MilestoneCardProps> = ({ item }) => {
  const { actor, content, createdAt } = item;

  return (
    <div className="bg-gradient-to-br from-orange-500/10 via-amber-500/5 to-transparent dark:from-orange-950/30 dark:via-slate-900 dark:to-slate-900 rounded-3xl border border-orange-500/30 dark:border-orange-800/50 p-5 shadow-xs transition-all hover:shadow-md mb-4">
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-3">
          <img
            src={
              actor.avatarUrl ||
              'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
            }
            alt={actor.fullName}
            className="w-10 h-10 rounded-full object-cover ring-2 ring-orange-500/40"
          />
          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-bold text-slate-900 dark:text-white text-sm">
                {actor.fullName}
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                vừa chạm mốc cột mốc mới!
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

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-orange-100 dark:bg-orange-900/60 text-orange-700 dark:text-orange-300 font-semibold text-xs border border-orange-300 dark:border-orange-700">
          <Trophy className="w-3.5 h-3.5 text-orange-500" />
          <span>Cột mốc</span>
        </div>
      </div>

      <div className="bg-white/80 dark:bg-slate-800/80 backdrop-blur-xs rounded-2xl p-4 border border-orange-500/20 dark:border-orange-900/50 flex items-center gap-3.5">
        <div className="p-3 rounded-2xl bg-orange-500 text-white shadow-md shadow-orange-500/30">
          <Flame className="w-6 h-6 animate-pulse" />
        </div>
        <div>
          <h4 className="font-bold text-slate-900 dark:text-white text-base">
            {content.title || 'Chuỗi ngày học tập chăm chỉ!'}
          </h4>
          <p className="text-xs text-slate-600 dark:text-slate-300 mt-0.5">
            {content.description ||
              `Đã duy trì liên tục chuỗi ${content.streakCount || 7} ngày luyện tập câu hỏi trắc nghiệm trên SQB.`}
          </p>
        </div>
      </div>
    </div>
  );
};
