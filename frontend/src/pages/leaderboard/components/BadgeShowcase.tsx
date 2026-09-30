import { useState } from 'react';
import type { BadgeResponseDto, UserBadgeResponseDto, BadgeTier } from '@/types/gamification.types';
import { getBadgeTriggerLabel } from '@/types/gamification.types';
import { Award, Lock, Sparkles, CheckCircle2, Users, Filter, Tag } from 'lucide-react';

interface BadgeShowcaseProps {
  allBadges: BadgeResponseDto[];
  userBadges: UserBadgeResponseDto[];
}

type StatusFilter = 'ALL' | 'UNLOCKED' | 'LOCKED';
type TierFilter = 'ALL' | BadgeTier;

export function BadgeShowcase({ allBadges, userBadges }: BadgeShowcaseProps) {
  const [statusFilter, setStatusFilter] = useState<StatusFilter>('ALL');
  const [tierFilter, setTierFilter] = useState<TierFilter>('ALL');

  const earnedBadgeMap = new Map<number, UserBadgeResponseDto>(
    userBadges.map((ub) => [ub.badgeId, ub])
  );

  const formatEarnedDate = (dateStr?: string) => {
    if (!dateStr) return '';
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return d.toLocaleDateString('vi-VN', {
      day: '2-digit',
      month: '2-digit',
      year: 'numeric',
    });
  };

  const getTierStyles = (tier: BadgeTier, unlocked: boolean) => {
    if (!unlocked) {
      return {
        cardBg: 'bg-slate-50 dark:bg-slate-900 border-slate-200/80 dark:border-slate-800 opacity-90 hover:opacity-100',
        badgeBg: 'bg-slate-200 dark:bg-slate-800 text-slate-400 dark:text-slate-500',
        textColor: 'text-slate-500 dark:text-slate-400',
      };
    }
    switch (tier) {
      case 'PLATINUM':
        return {
          cardBg: 'bg-linear-to-br from-indigo-50/80 via-purple-50/60 to-pink-50/40 dark:from-purple-950/40 dark:via-slate-900 dark:to-slate-900 border-purple-200 dark:border-purple-800/40 shadow-xs hover:shadow-md',
          badgeBg: 'bg-linear-to-tr from-indigo-500 to-purple-600 text-white shadow-sm shadow-purple-500/30',
          textColor: 'text-purple-700 dark:text-purple-300',
        };
      case 'GOLD':
        return {
          cardBg: 'bg-linear-to-br from-amber-50/80 via-yellow-50/60 to-orange-50/40 dark:from-amber-950/40 dark:via-slate-900 dark:to-slate-900 border-amber-200 dark:border-amber-800/40 shadow-xs hover:shadow-md',
          badgeBg: 'bg-linear-to-tr from-amber-400 to-yellow-600 text-white shadow-sm shadow-amber-500/30',
          textColor: 'text-amber-800 dark:text-amber-300',
        };
      case 'SILVER':
        return {
          cardBg: 'bg-linear-to-br from-slate-100 via-slate-50 to-slate-200/50 dark:from-slate-800/80 dark:via-slate-900 dark:to-slate-900 border-slate-300 dark:border-slate-700 shadow-xs hover:shadow-md',
          badgeBg: 'bg-linear-to-tr from-slate-400 to-slate-600 text-white shadow-sm',
          textColor: 'text-slate-700 dark:text-slate-200',
        };
      case 'BRONZE':
      default:
        return {
          cardBg: 'bg-linear-to-br from-orange-50/80 to-amber-50/60 dark:from-amber-950/30 dark:via-slate-900 dark:to-slate-900 border-orange-200 dark:border-orange-800/40 shadow-xs hover:shadow-md',
          badgeBg: 'bg-linear-to-tr from-orange-400 to-amber-600 text-white shadow-sm',
          textColor: 'text-orange-800 dark:text-orange-300',
        };
    }
  };

  // Filter badges
  const filteredBadges = allBadges.filter((badge) => {
    const isUnlocked = earnedBadgeMap.has(badge.id);
    if (statusFilter === 'UNLOCKED' && !isUnlocked) return false;
    if (statusFilter === 'LOCKED' && isUnlocked) return false;
    if (tierFilter !== 'ALL' && badge.badgeTier !== tierFilter) return false;
    return true;
  });

  const unlockPercent = Math.round((userBadges.length / Math.max(1, allBadges.length)) * 100);

  return (
    <div className="space-y-6">
      {/* Header Summary & Progress Banner */}
      <div className="bg-white dark:bg-slate-900 p-6 rounded-3xl border border-slate-200/80 dark:border-slate-800 shadow-xs flex flex-col md:flex-row items-center justify-between gap-6">
        <div className="space-y-1.5 max-w-xl text-center md:text-left">
          <h3 className="text-lg sm:text-xl font-bold text-slate-900 dark:text-slate-100 flex items-center justify-center md:justify-start gap-2">
            <Sparkles className="w-5 h-5 text-amber-500" />
            Bộ Sưu Tập Huy Hiệu Vinh Danh
          </h3>
          <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400 leading-relaxed">
            Mở khóa các danh hiệu cao quý bằng cách đóng góp câu hỏi, đạt top xếp hạng học kỳ và tích cực giải đáp thắc mắc chuyên môn.
          </p>
        </div>

        {/* Progress Capsule */}
        <div className="w-full md:w-64 p-4 bg-indigo-50/70 dark:bg-indigo-950/40 rounded-2xl border border-indigo-100 dark:border-indigo-900/40 space-y-2 shrink-0">
          <div className="flex items-center justify-between text-xs">
            <span className="font-semibold text-indigo-900 dark:text-indigo-200">Tiến độ mở khóa</span>
            <span className="font-extrabold text-indigo-700 dark:text-indigo-300">
              {userBadges.length}/{allBadges.length} ({unlockPercent}%)
            </span>
          </div>
          <div className="w-full h-2.5 bg-indigo-100 dark:bg-slate-800 rounded-full overflow-hidden">
            <div
              className="h-full bg-linear-to-r from-indigo-500 to-purple-600 rounded-full transition-all duration-500"
              style={{ width: `${unlockPercent}%` }}
            />
          </div>
        </div>
      </div>

      {/* Filter Toolbar */}
      <div className="flex flex-wrap items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
        {/* Status Filter */}
        <div className="flex items-center gap-1.5">
          <button
            type="button"
            onClick={() => setStatusFilter('ALL')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              statusFilter === 'ALL'
                ? 'bg-slate-900 dark:bg-indigo-600 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            Tất cả ({allBadges.length})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter('UNLOCKED')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              statusFilter === 'UNLOCKED'
                ? 'bg-emerald-600 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            Đã mở ({userBadges.length})
          </button>
          <button
            type="button"
            onClick={() => setStatusFilter('LOCKED')}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all cursor-pointer ${
              statusFilter === 'LOCKED'
                ? 'bg-slate-700 dark:bg-slate-700 text-white'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 hover:bg-slate-200 dark:hover:bg-slate-700'
            }`}
          >
            Chưa đạt ({allBadges.length - userBadges.length})
          </button>
        </div>

        {/* Tier Filter */}
        <div className="flex items-center gap-1.5 overflow-x-auto">
          <span className="text-xs text-slate-400 dark:text-slate-500 flex items-center gap-1 mr-1">
            <Filter className="w-3.5 h-3.5" />
            Hạng:
          </span>
          {(['ALL', 'PLATINUM', 'GOLD', 'SILVER', 'BRONZE'] as const).map((tier) => (
            <button
              key={tier}
              type="button"
              onClick={() => setTierFilter(tier)}
              className={`px-2.5 py-1 rounded-lg text-[11px] font-bold transition-all cursor-pointer uppercase ${
                tierFilter === tier
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-slate-50 dark:bg-slate-800 text-slate-500 dark:text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-700'
              }`}
            >
              {tier === 'ALL' ? 'Tất cả' : tier}
            </button>
          ))}
        </div>
      </div>

      {/* Grid Badges */}
      {filteredBadges.length === 0 ? (
        <div className="bg-white dark:bg-slate-900 p-12 rounded-3xl border border-slate-200 dark:border-slate-800 text-center space-y-2">
          <Award className="w-12 h-12 text-slate-300 dark:text-slate-600 mx-auto" />
          <h4 className="font-bold text-slate-700 dark:text-slate-200">Không có huy hiệu phù hợp bộ lọc</h4>
          <p className="text-xs text-slate-400 dark:text-slate-500">Hãy thử đổi tiêu chí lọc hoặc điều kiện mở khóa.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {filteredBadges.map((badge) => {
            const userBadge = earnedBadgeMap.get(badge.id);
            const isUnlocked = !!userBadge;
            const style = getTierStyles(badge.badgeTier, isUnlocked);

            return (
              <div
                key={badge.id}
                className={`p-5 rounded-3xl border transition-all duration-200 flex flex-col justify-between relative group ${style.cardBg}`}
              >
                <div>
                  <div className="flex items-start justify-between gap-3 mb-3">
                    <div className={`w-12 h-12 rounded-2xl flex items-center justify-center text-xl shrink-0 overflow-hidden ${style.badgeBg}`}>
                      {badge.iconUrl ? (
                        <img src={badge.iconUrl} alt={badge.name} className="w-8 h-8 object-contain" />
                      ) : isUnlocked ? (
                        <Award className="w-6 h-6" />
                      ) : (
                        <Lock className="w-5 h-5" />
                      )}
                    </div>

                    <div className="flex items-center gap-1.5">
                      {badge.badgeTriggerEvent && (
                        <span className="text-[10px] font-medium px-2 py-0.5 rounded-md bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-200/80 dark:border-slate-700 flex items-center gap-1">
                          <Tag className="w-2.5 h-2.5" />
                          {getBadgeTriggerLabel(badge.badgeTriggerEvent)}
                        </span>
                      )}
                      <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-full uppercase tracking-wider ${style.textColor} bg-white/80 dark:bg-slate-900/80 border border-current/20`}>
                        {badge.badgeTier}
                      </span>
                    </div>
                  </div>

                  <h4 className={`font-bold text-sm sm:text-base ${isUnlocked ? 'text-slate-900 dark:text-slate-100' : 'text-slate-700 dark:text-slate-200'}`}>{badge.name}</h4>
                  <p className={`text-xs mt-1 leading-relaxed ${isUnlocked ? 'text-slate-600 dark:text-slate-300' : 'text-slate-500 dark:text-slate-400'}`}>{badge.description}</p>
                </div>

                {/* Status or Criteria Details */}
                <div className="mt-5 pt-3 border-t border-slate-200/60 dark:border-slate-800 space-y-2">
                  {isUnlocked && userBadge ? (
                    <div className="flex items-center justify-between text-xs">
                      <div className="flex items-center gap-1.5 font-bold text-emerald-600 dark:text-emerald-400">
                        <CheckCircle2 className="w-4 h-4" />
                        <span>Đã mở khóa</span>
                      </div>
                      <span className="text-[11px] text-slate-500 dark:text-slate-400 font-mono">
                        {formatEarnedDate(userBadge.earnedAt)}
                      </span>
                    </div>
                  ) : (
                    <div className="flex items-center justify-between text-xs text-slate-400 dark:text-slate-500">
                      <span className="flex items-center gap-1">
                        <Lock className="w-3.5 h-3.5" />
                        Chưa mở khóa
                      </span>
                      {badge.totalEarnedUsers !== undefined && (
                        <span className="flex items-center gap-1 text-[11px] text-slate-400 dark:text-slate-500">
                          <Users className="w-3 h-3" />
                          {badge.totalEarnedUsers} bạn đã đạt
                        </span>
                      )}
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
}

