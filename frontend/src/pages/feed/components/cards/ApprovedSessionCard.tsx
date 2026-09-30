import type { ActivityFeedItemDto } from '@/types/activityFeed.types';
import { CheckCircle2, BookOpen, Zap, Coins, Sparkles, ExternalLink, Award } from 'lucide-react';

interface ApprovedSessionCardProps {
  item: ActivityFeedItemDto;
}

export function ApprovedSessionCard({ item }: ApprovedSessionCardProps) {
  return (
    <article className="rounded-2xl border border-emerald-300/80 dark:border-emerald-800/60 bg-linear-to-br from-emerald-50/40 via-white to-teal-50/30 dark:from-emerald-950/20 dark:via-slate-900 dark:to-teal-950/20 p-4 sm:p-5 shadow-xs space-y-3.5 transition-shadow hover:shadow-md">
      {/* Header: Congratulation Banner */}
      <div className="flex items-start justify-between gap-3">
        <div className="flex items-center gap-3">
          {/* Avatar with success checkmark */}
          <div className="relative shrink-0">
            <div className="w-10 h-10 rounded-full bg-linear-to-tr from-emerald-500 to-teal-600 flex items-center justify-center text-white font-bold text-sm shadow-xs overflow-hidden">
              {item.actor.avatarUrl ? (
                <img src={item.actor.avatarUrl} alt={item.actor.fullName} className="w-full h-full object-cover" />
              ) : (
                item.actor.fullName.charAt(0)
              )}
            </div>
            <div className="absolute -bottom-1 -right-1 w-5 h-5 rounded-full bg-emerald-600 text-white flex items-center justify-center ring-2 ring-white dark:ring-slate-900 shadow-xs">
              <CheckCircle2 className="w-3 h-3" />
            </div>
          </div>

          <div>
            <div className="flex flex-wrap items-center gap-2">
              <h4 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100 leading-tight">
                {item.actor.fullName}
              </h4>
              <span className="text-[10px] font-extrabold bg-emerald-100 dark:bg-emerald-950/60 text-emerald-800 dark:text-emerald-300 px-2 py-0.5 rounded-full border border-emerald-300 dark:border-emerald-800/60 flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-emerald-600 dark:text-emerald-400" />
                Đề xuất được phê duyệt
              </span>
            </div>

            <div className="flex items-center gap-1.5 text-xs text-slate-400 dark:text-slate-500 mt-0.5">
              <span>{item.createdAt}</span>
              <span>•</span>
              <span className="text-emerald-700 dark:text-emerald-400 font-semibold">Ngân hàng đề thi HUST</span>
            </div>
          </div>
        </div>
      </div>

      {/* Main Content Box */}
      <div className="p-4 rounded-xl bg-white dark:bg-slate-800/90 border border-emerald-200/70 dark:border-emerald-800/40 space-y-2.5 shadow-2xs">
        <div className="flex flex-wrap items-center justify-between gap-2">
          <div className="flex items-center gap-2">
            <BookOpen className="w-4 h-4 text-emerald-600 dark:text-emerald-400" />
            <span className="text-xs font-bold text-slate-800 dark:text-slate-200">
              {item.content.subject_code} • {item.content.subject_name || 'Môn học chuyên ngành'}
            </span>
          </div>

          {/* Rewards Badge */}
          <div className="flex items-center gap-2">
            <span className="px-2 py-0.5 bg-amber-50 dark:bg-amber-950/40 border border-amber-200 dark:border-amber-800/50 text-amber-800 dark:text-amber-300 rounded-lg text-xs font-bold flex items-center gap-1">
              <Coins className="w-3 h-3 text-amber-500 fill-amber-400" />
              +15 xu
            </span>
            <span className="px-2 py-0.5 bg-indigo-50 dark:bg-indigo-950/40 border border-indigo-200 dark:border-indigo-800/50 text-indigo-700 dark:text-indigo-300 rounded-lg text-xs font-bold flex items-center gap-1">
              <Zap className="w-3 h-3 text-indigo-600 fill-indigo-500" />
              +50 pts
            </span>
          </div>
        </div>

        <h3 className="font-bold text-sm sm:text-base text-slate-900 dark:text-slate-100 leading-snug">
          {item.content.title || 'Bộ đề trắc nghiệm chuyên đề: Phân tích & Thiết kế thuật toán'}
        </h3>
        
        <p className="text-xs sm:text-sm text-slate-600 dark:text-slate-300 leading-relaxed">
          {item.content.description ||
            'Hội đồng Giảng viên đã thẩm định và chính thức đưa 5 câu hỏi trắc nghiệm mới vào hệ thống khảo thí chung toàn trường.'}
        </p>
      </div>

      {/* Action Footer */}
      <div className="flex items-center justify-between text-xs pt-1">
        <span className="text-slate-500 dark:text-slate-400 flex items-center gap-1">
          <Award className="w-3.5 h-3.5 text-amber-500" />
          Được ghi nhận vào điểm cống hiến học kỳ 2026.1
        </span>

        <button
          type="button"
          className="text-xs font-bold text-emerald-700 dark:text-emerald-400 hover:text-emerald-800 dark:hover:text-emerald-300 flex items-center gap-1 cursor-pointer"
        >
          <span>Xem câu hỏi đã duyệt</span>
          <ExternalLink className="w-3.5 h-3.5" />
        </button>
      </div>
    </article>
  );
}
