import { useState } from 'react';
import type { ActivityFeedItemDto } from '@/types/activityFeed.types';
import { Target, Zap, Trophy, Sparkles } from 'lucide-react';

interface ReachedMilestoneCardProps {
  item: ActivityFeedItemDto;
}

export function ReachedMilestoneCard({ item }: ReachedMilestoneCardProps) {
  const [cheers, setCheers] = useState(25);
  const [hasCheered, setHasCheered] = useState(false);

  const handleToggleCheer = () => {
    setHasCheered(!hasCheered);
    setCheers(hasCheered ? cheers - 1 : cheers + 1);
  };

  return (
    <article className="rounded-2xl border border-purple-200/80 dark:border-purple-800/50 bg-linear-to-b from-purple-50/40 via-white to-indigo-50/20 dark:from-purple-950/20 dark:via-slate-900 dark:to-indigo-950/20 p-4 sm:p-5 shadow-xs space-y-3.5 transition-shadow hover:shadow-md">
      {/* Header */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-full bg-linear-to-tr from-purple-500 to-indigo-600 flex items-center justify-center text-white font-bold text-sm shadow-xs overflow-hidden shrink-0">
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
              <span className="text-[10px] font-extrabold bg-purple-100 dark:bg-purple-950/60 text-purple-800 dark:text-purple-300 px-2 py-0.5 rounded-full border border-purple-300 dark:border-purple-700/60 flex items-center gap-1">
                <Target className="w-3 h-3 text-purple-600 dark:text-purple-400" />
                Cột mốc thành tựu
              </span>
            </div>

            <p className="text-xs text-slate-400 dark:text-slate-500 mt-0.5">{item.createdAt}</p>
          </div>
        </div>
      </div>

      {/* Milestone Box */}
      <div className="p-4 rounded-xl bg-linear-to-r from-purple-900 to-indigo-900 text-white space-y-2 shadow-md">
        <div className="flex items-center justify-between">
          <span className="text-[10px] font-bold text-amber-300 uppercase tracking-wider flex items-center gap-1">
            <Trophy className="w-3 h-3 fill-amber-300" />
            Thành tích cống hiến
          </span>
          <span className="px-2 py-0.5 bg-white/20 rounded-full text-[11px] font-bold text-white flex items-center gap-1">
            <Zap className="w-3 h-3 text-amber-300 fill-amber-300" />
            1,000+ Điểm
          </span>
        </div>

        <h3 className="font-bold text-sm sm:text-base text-white">
          {item.content.title || 'Chinh phục cột mốc 1,000 điểm cống hiến học thuật'}
        </h3>

        <p className="text-xs text-purple-200 leading-relaxed">
          {item.content.description ||
            'Chúc mừng bạn đã tích lũy hơn 1,000 điểm cống hiến thông qua việc giải đề, đóng góp câu hỏi và chia sẻ tài liệu hữu ích!'}
        </p>
      </div>

      {/* Bottom Action */}
      <div className="pt-2 border-t border-slate-100 dark:border-slate-800 flex items-center justify-between">
        <button
          type="button"
          onClick={handleToggleCheer}
          className={`flex items-center gap-1.5 px-3.5 py-1.5 rounded-xl text-xs sm:text-sm font-bold transition-all cursor-pointer ${
            hasCheered
              ? 'bg-purple-100 dark:bg-purple-950/60 text-purple-700 dark:text-purple-300 ring-1 ring-purple-300 dark:ring-purple-700/60'
              : 'bg-slate-100 dark:bg-slate-800 hover:bg-slate-200/80 dark:hover:bg-slate-750 text-slate-700 dark:text-slate-200'
          }`}
        >
          <Sparkles className={`w-4 h-4 ${hasCheered ? 'text-purple-600' : ''}`} />
          <span>{hasCheered ? 'Đã thả sao' : 'Cổ vũ bạn ấy'}</span>
          <span className="ml-1 text-slate-400 dark:text-slate-500 font-normal">({cheers})</span>
        </button>

        <span className="text-[11px] text-slate-400 dark:text-slate-500">Tiếp tục tiến tới Top 3 học kỳ!</span>
      </div>
    </article>
  );
}
