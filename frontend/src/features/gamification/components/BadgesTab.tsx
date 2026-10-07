import { useState } from 'react';
import { Award, Lock, Users, Sparkles, X } from 'lucide-react';
import { useAllBadges, useMyBadges } from '../hooks/useGamification';
import type { BadgeTier, BadgeResponseDto } from '../types/gamification.types';
import { getTierColor } from '../types/gamification.types';

export function BadgesTab() {
  const [viewMode, setViewMode] = useState<'my' | 'all'>('my');
  const [selectedTier, setSelectedTier] = useState<BadgeTier | undefined>(undefined);
  const [selectedBadge, setSelectedBadge] = useState<BadgeResponseDto | null>(null);

  const { data: allBadges = [], isLoading: isAllLoading } = useAllBadges();
  const { data: myBadges = [], isLoading: isMyLoading } = useMyBadges();

  const myBadgeIdSet = new Set(myBadges.map((b) => b.badgeId));

  const filteredAllBadges = allBadges.filter((b) => {
    if (selectedTier && b.badgeTier !== selectedTier) return false;
    return true;
  });

  const filteredMyBadges = myBadges.filter((b) => {
    if (selectedTier && b.badgeTier !== selectedTier) return false;
    return true;
  });

  const tiers: BadgeTier[] = ['BRONZE', 'SILVER', 'GOLD', 'PLATINUM', 'DIAMOND'];

  return (
    <div className="space-y-6 animate-in fade-in duration-300">
      {/* Controls Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 p-3 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs">
        {/* Toggle Mode */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-900 rounded-xl">
          <button
            type="button"
            onClick={() => setViewMode('my')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              viewMode === 'my'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Award className="w-4 h-4" />
            <span>Huy Hiệu Của Tôi</span>
            <span className="px-1.5 py-0.2 rounded-full text-[10px] font-black bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
              {myBadges.length}
            </span>
          </button>

          <button
            type="button"
            onClick={() => setViewMode('all')}
            className={`flex items-center gap-2 px-5 py-2.5 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              viewMode === 'all'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            <Sparkles className="w-4 h-4" />
            <span>Toàn Bộ Danh Mục</span>
            <span className="px-1.5 py-0.2 rounded-full text-[10px] font-black bg-slate-200 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
              {allBadges.length}
            </span>
          </button>
        </div>

        {/* Tier Filter Pills */}
        <div className="flex items-center gap-1.5 overflow-x-auto scrollbar-none">
          <button
            type="button"
            onClick={() => setSelectedTier(undefined)}
            className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
              !selectedTier
                ? 'bg-indigo-600 text-white shadow-xs'
                : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200'
            }`}
          >
            Tất cả cấp
          </button>
          {tiers.map((tier) => (
            <button
              key={tier}
              type="button"
              onClick={() => setSelectedTier(tier)}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-colors cursor-pointer ${
                selectedTier === tier
                  ? 'bg-indigo-600 text-white shadow-xs'
                  : 'bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 hover:bg-slate-200'
              }`}
            >
              {tier}
            </button>
          ))}
        </div>
      </div>

      {/* Grid of Badges */}
      {viewMode === 'my' ? (
        /* MY BADGES */
        <div>
          {isMyLoading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              {Array.from({ length: 6 }).map((_, i) => (
                <div key={i} className="h-44 rounded-3xl bg-slate-100 dark:bg-slate-800/80 animate-pulse" />
              ))}
            </div>
          ) : filteredMyBadges.length === 0 ? (
            <div className="py-16 text-center text-slate-400 bg-white dark:bg-slate-800/50 rounded-3xl border border-slate-200 dark:border-slate-800">
              <Award className="w-12 h-12 mx-auto mb-3 opacity-30" />
              <p className="font-bold text-base text-slate-700 dark:text-slate-300">
                Chưa có huy hiệu nào trong danh mục này
              </p>
              <p className="text-xs text-slate-500 mt-1">
                Hãy tích cực đóng góp câu hỏi, luyện tập và điểm danh mỗi ngày để chinh phục các danh hiệu nhé!
              </p>
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
              {filteredMyBadges.map((badge) => {
                const tierStyle = getTierColor(badge.badgeTier);

                return (
                  <div
                    key={badge.badgeId}
                    className={`relative p-5 rounded-3xl border ${tierStyle.badgeBg} ${tierStyle.badgeBorder} flex flex-col items-center text-center justify-between transition-all hover:scale-105 shadow-xs`}
                  >
                    <div className="relative my-2">
                      <div className="w-18 h-18 sm:w-20 sm:h-20 rounded-2xl flex items-center justify-center shadow-lg bg-linear-to-tr from-white to-slate-100 dark:from-slate-800 dark:to-slate-700 p-2">
                        {badge.iconUrl ? (
                          <img src={badge.iconUrl} alt={badge.name} className="w-full h-full object-contain" />
                        ) : (
                          <Award className={`w-10 h-10 ${tierStyle.textColor}`} />
                        )}
                      </div>
                      <span className="absolute -bottom-2 -right-1 px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider bg-emerald-500 text-white shadow-xs">
                        Đã Đạt
                      </span>
                    </div>

                    <div className="space-y-1 mt-2">
                      <h4 className="font-black text-sm text-slate-900 dark:text-white line-clamp-1">
                        {badge.name}
                      </h4>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2">
                        {badge.description}
                      </p>
                    </div>

                    <div className="mt-3 pt-2 border-t border-slate-200/60 dark:border-slate-700/60 w-full flex items-center justify-between text-[10px] text-slate-400">
                      <span className={`font-black uppercase tracking-wider ${tierStyle.textColor}`}>
                        {badge.badgeTier}
                      </span>
                      <span>{new Date(badge.earnedAt).toLocaleDateString('vi-VN')}</span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      ) : (
        /* ALL SYSTEM BADGES */
        <div>
          {isAllLoading ? (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4">
              {Array.from({ length: 8 }).map((_, i) => (
                <div key={i} className="h-44 rounded-3xl bg-slate-100 dark:bg-slate-800/80 animate-pulse" />
              ))}
            </div>
          ) : (
            <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-4 gap-4 sm:gap-5">
              {filteredAllBadges.map((badge) => {
                const isEarned = myBadgeIdSet.has(badge.badgeId);
                const tierStyle = getTierColor(badge.badgeTier);

                return (
                  <div
                    key={badge.badgeId}
                    onClick={() => setSelectedBadge(badge)}
                    className={`relative p-5 rounded-3xl border flex flex-col items-center text-center justify-between transition-all hover:scale-105 cursor-pointer shadow-xs ${
                      isEarned
                        ? `${tierStyle.badgeBg} ${tierStyle.badgeBorder}`
                        : 'bg-white dark:bg-slate-800/80 border-slate-200 dark:border-slate-700 opacity-75 hover:opacity-100'
                    }`}
                  >
                    <div className="relative my-2">
                      <div
                        className={`w-18 h-18 sm:w-20 sm:h-20 rounded-2xl flex items-center justify-center p-2 shadow-sm ${
                          isEarned
                            ? 'bg-linear-to-tr from-white to-slate-100 dark:from-slate-800 dark:to-slate-700'
                            : 'bg-slate-100 dark:bg-slate-900 grayscale opacity-50'
                        }`}
                      >
                        {badge.iconUrl ? (
                          <img src={badge.iconUrl} alt={badge.name} className="w-full h-full object-contain" />
                        ) : (
                          <Award className="w-10 h-10 text-slate-400" />
                        )}
                      </div>

                      {isEarned ? (
                        <span className="absolute -bottom-2 -right-1 px-2 py-0.5 rounded-full text-[9px] font-black uppercase tracking-wider bg-emerald-500 text-white shadow-xs">
                          Đã Đạt
                        </span>
                      ) : (
                        <span className="absolute -bottom-2 -right-1 p-1 rounded-full bg-slate-500 text-white shadow-xs">
                          <Lock className="w-2.5 h-2.5" />
                        </span>
                      )}
                    </div>

                    <div className="space-y-1 mt-2">
                      <h4 className="font-black text-sm text-slate-900 dark:text-white line-clamp-1">
                        {badge.name}
                      </h4>
                      <p className="text-[11px] text-slate-500 dark:text-slate-400 line-clamp-2">
                        {badge.description}
                      </p>
                    </div>

                    <div className="mt-3 pt-2 border-t border-slate-200/60 dark:border-slate-700/60 w-full flex items-center justify-between text-[10px] text-slate-400">
                      <span className={`font-black uppercase tracking-wider ${tierStyle.textColor}`}>
                        {badge.badgeTier}
                      </span>
                      <span className="flex items-center gap-1">
                        <Users className="w-3 h-3" />
                        {badge.totalEarnedUsers ?? 0}
                      </span>
                    </div>
                  </div>
                );
              })}
            </div>
          )}
        </div>
      )}

      {/* Badge Detail Modal */}
      {selectedBadge && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs animate-in fade-in duration-200">
          <div className="relative w-full max-w-sm rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-2xl p-6 text-center space-y-4">
            <button
              type="button"
              onClick={() => setSelectedBadge(null)}
              className="absolute top-4 right-4 p-1.5 rounded-xl text-slate-400 hover:text-slate-600 dark:hover:text-slate-200 hover:bg-slate-100 dark:hover:bg-slate-800 transition-colors cursor-pointer"
            >
              <X className="w-4 h-4" />
            </button>

            <div className="w-24 h-24 mx-auto rounded-3xl bg-indigo-50 dark:bg-indigo-950 p-3 shadow-md flex items-center justify-center">
              {selectedBadge.iconUrl ? (
                <img src={selectedBadge.iconUrl} alt={selectedBadge.name} className="w-full h-full object-contain" />
              ) : (
                <Award className="w-12 h-12 text-indigo-500" />
              )}
            </div>

            <div className="space-y-1">
              <span className="px-2.5 py-0.5 rounded-full text-[10px] font-black uppercase tracking-wider bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
                Cấp {selectedBadge.badgeTier}
              </span>
              <h3 className="text-lg font-black text-slate-900 dark:text-white">
                {selectedBadge.name}
              </h3>
              <p className="text-xs text-slate-500 dark:text-slate-400">
                {selectedBadge.description}
              </p>
            </div>

            <div className="p-3 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/60 text-xs space-y-1 text-slate-600 dark:text-slate-300 text-left">
              <div className="flex items-center justify-between">
                <span>Trạng thái:</span>
                <span className="font-bold">
                  {myBadgeIdSet.has(selectedBadge.badgeId) ? '✅ Đã mở khóa' : '🔒 Chưa đạt'}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span>Số người đã đạt:</span>
                <span className="font-bold">{selectedBadge.totalEarnedUsers ?? 0} sinh viên</span>
              </div>
            </div>

            <button
              type="button"
              onClick={() => setSelectedBadge(null)}
              className="w-full py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-200 font-bold text-xs transition-colors cursor-pointer"
            >
              Đóng
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
