import React, { useState } from 'react';
import type { ActivityFeedItemDto } from '../../types/feed.types';
import { Award, Sparkles, Heart } from 'lucide-react';

interface EarnedBadgeCardProps {
  item: ActivityFeedItemDto;
}

export const EarnedBadgeCard: React.FC<EarnedBadgeCardProps> = ({ item }) => {
  const { actor, content, createdAt } = item;
  const [hasCheered, setHasCheered] = useState(false);
  const [cheerCount, setCheerCount] = useState(content.reactCount || 4);

  const handleCheer = () => {
    if (!hasCheered) {
      setHasCheered(true);
      setCheerCount((c) => c + 1);
    } else {
      setHasCheered(false);
      setCheerCount((c) => c - 1);
    }
  };

  return (
    <div className="bg-gradient-to-br from-amber-500/10 via-purple-500/5 to-transparent dark:from-amber-950/30 dark:via-slate-900 dark:to-slate-900 rounded-3xl border border-amber-500/30 dark:border-amber-800/50 p-5 shadow-xs transition-all hover:shadow-md mb-4">
      {/* Header */}
      <div className="flex items-center justify-between mb-3.5">
        <div className="flex items-center gap-3">
          <img
            src={
              actor.avatarUrl ||
              'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'
            }
            alt={actor.fullName}
            className="w-10 h-10 rounded-full object-cover ring-2 ring-amber-500/40"
          />
          <div>
            <div className="flex items-center gap-1.5 flex-wrap">
              <span className="font-bold text-slate-900 dark:text-white text-sm">
                {actor.fullName}
              </span>
              <span className="text-xs text-slate-500 dark:text-slate-400">
                vừa mở khóa huy hiệu mới!
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

        <div className="flex items-center gap-1.5 px-3 py-1 rounded-full bg-amber-100 dark:bg-amber-900/60 text-amber-700 dark:text-amber-300 font-semibold text-xs border border-amber-300 dark:border-amber-700">
          <Sparkles className="w-3.5 h-3.5 text-amber-500" />
          <span>Thành tích mới</span>
        </div>
      </div>

      {/* Badge Showcase Box */}
      <div className="flex items-center gap-4 bg-white/80 dark:bg-slate-800/80 backdrop-blur-xs rounded-2xl p-4 border border-amber-500/20 dark:border-amber-900/50 mb-3.5">
        <div className="w-16 h-16 shrink-0 rounded-2xl bg-gradient-to-tr from-amber-400 to-amber-200 flex items-center justify-center shadow-lg shadow-amber-500/20 text-3xl">
          {content.badge_icon_url ? (
            <img
              src={content.badge_icon_url}
              alt={content.badge_name || 'Huy hiệu'}
              className="w-12 h-12 object-contain"
            />
          ) : (
            <Award className="w-9 h-9 text-amber-800" />
          )}
        </div>

        <div>
          <span className="text-[11px] font-semibold tracking-wider uppercase text-amber-600 dark:text-amber-400">
            Huy hiệu vinh danh
          </span>
          <h4 className="font-bold text-slate-900 dark:text-white text-base">
            {content.badge_name || 'Chiến binh SoICT'}
          </h4>
          <p className="text-xs text-slate-600 dark:text-slate-300 mt-0.5">
            {content.description || 'Đã xuất sắc hoàn thành chuỗi đóng góp câu hỏi chất lượng cao.'}
          </p>
        </div>
      </div>

      {/* Footer cheer action */}
      <div className="flex items-center justify-between pt-1">
        <span className="text-xs text-slate-400">
          {cheerCount > 0 ? `${cheerCount} bạn bè đã thả tim chúc mừng` : 'Hãy là người đầu tiên chúc mừng!'}
        </span>

        <button
          type="button"
          onClick={handleCheer}
          className={`flex items-center gap-1.5 px-4 py-1.5 rounded-full text-xs font-semibold transition-all cursor-pointer ${
            hasCheered
              ? 'bg-rose-500 text-white shadow-md shadow-rose-500/25 scale-105'
              : 'bg-slate-100 dark:bg-slate-800 hover:bg-rose-50 dark:hover:bg-rose-950/30 text-slate-700 dark:text-slate-300 hover:text-rose-500'
          }`}
        >
          <Heart className={`w-3.5 h-3.5 ${hasCheered ? 'fill-current' : ''}`} />
          <span>{hasCheered ? 'Đã chúc mừng' : 'Chúc mừng! 🎉'}</span>
        </button>
      </div>
    </div>
  );
};
