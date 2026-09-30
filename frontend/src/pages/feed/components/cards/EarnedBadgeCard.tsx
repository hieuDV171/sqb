import { useState } from 'react';
import type { ActivityFeedItemDto } from '@/types/activityFeed.types';
import { Award, Sparkles, Heart, Crown } from 'lucide-react';

interface EarnedBadgeCardProps {
  item: ActivityFeedItemDto;
}

export function EarnedBadgeCard({ item }: EarnedBadgeCardProps) {
  const [congrats, setCongrats] = useState(32);
  const [hasCongratulated, setHasCongratulated] = useState(false);

  const handleToggleCongrats = () => {
    setHasCongratulated(!hasCongratulated);
    setCongrats(hasCongratulated ? congrats - 1 : congrats + 1);
  };

  return (
    <article className="rounded-2xl border border-amber-300/80 dark:border-amber-700/50 bg-linear-to-b from-amber-50/50 via-white to-yellow-50/30 dark:from-amber-950/20 dark:via-slate-900 dark:to-yellow-950/20 p-4 sm:p-5 shadow-xs space-y-3.5 transition-shadow hover:shadow-md">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-linear-to-tr from-amber-400 to-yellow-600 flex items-center justify-center text-white font-bold text-sm shadow-xs overflow-hidden shrink-0">
            {item.actor.avatarUrl ? (
              <img src={item.actor.avatarUrl} alt={item.actor.fullName} className="w-full h-full object-cover" />
            ) : (
              item.actor.fullName.charAt(0)
            )}
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100 leading-tight">
                {item.actor.fullName}
              </h4>
              <span className="text-[10px] font-extrabold bg-amber-100 dark:bg-amber-950/60 text-amber-800 dark:text-amber-300 px-2 py-0.5 rounded-full border border-amber-300 dark:border-amber-700/60 flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-amber-600 dark:text-amber-400" />
                Mở khóa Huy hiệu mới
              </span>
            </div>

            <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">{item.createdAt}</p>
          </div>
        </div>
      </div>

      {/* Badge Showcase Highlight */}
      <div className="p-4 sm:p-5 rounded-2xl bg-linear-to-r from-amber-500/10 via-yellow-500/15 to-orange-500/10 dark:from-amber-950/30 dark:via-yellow-950/30 dark:to-orange-950/30 border border-amber-200 dark:border-amber-800/40 flex flex-col sm:flex-row items-center gap-4 text-center sm:text-left">
        {/* Glowing Badge Emblem */}
        <div className="relative shrink-0">
          <div className="w-16 h-16 rounded-2xl bg-linear-to-tr from-amber-400 to-yellow-600 flex items-center justify-center text-white shadow-lg shadow-amber-500/30 ring-4 ring-white dark:ring-slate-800">
            <Award className="w-9 h-9 fill-amber-100" />
          </div>
          <Crown className="w-5 h-5 text-amber-400 fill-amber-300 absolute -top-2 -right-1" />
        </div>

        <div className="space-y-1">
          <span className="text-[10px] font-bold text-amber-800 dark:text-amber-300 uppercase tracking-widest bg-amber-200/80 dark:bg-amber-900/60 px-2 py-0.5 rounded">
            Danh hiệu cao quý
          </span>
          <h3 className="font-extrabold text-base sm:text-lg text-slate-900 dark:text-slate-100">
            {item.content.badge_name || item.content.title || 'Chiến Thần Đề Thi HUST'}
          </h3>
          <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
            {item.content.description ||
              'Đạt mốc giải thành công 50 đề thi thử với độ chính xác trên 85%. Chúc mừng tinh thần học tập xuất sắc!'}
          </p>
        </div>
      </div>

      {/* Bottom Action: Congratulate Button */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
        <button
          type="button"
          onClick={handleToggleCongrats}
          className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
            hasCongratulated
              ? 'bg-rose-50 dark:bg-rose-950/50 text-rose-600 dark:text-rose-400 ring-1 ring-rose-200 dark:ring-rose-800/50'
              : 'bg-slate-100 dark:bg-slate-800 hover:bg-slate-200/80 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200'
          }`}
        >
          <Heart className={`w-4 h-4 ${hasCongratulated ? 'fill-rose-500 text-rose-500' : ''}`} />
          <span>{hasCongratulated ? 'Đã chúc mừng' : 'Chúc mừng bạn ấy'}</span>
          <span className="ml-1 text-slate-400 dark:text-slate-500 font-normal">({congrats})</span>
        </button>

        <span className="text-[11px] text-slate-400 dark:text-slate-500 italic">
          Được ghi danh trên Bảng xếp hạng học kỳ
        </span>
      </div>
    </article>
  );
}
