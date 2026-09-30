import type { LeaderboardEntryDto, LeaderboardScope } from '@/types/gamification.types';
import { getRewardCoins } from '@/types/gamification.types';
import { Trophy, Medal, Award, Crown, Coins, Zap, Sparkles } from 'lucide-react';

interface PodiumProps {
  topUsers: LeaderboardEntryDto[];
  scope: LeaderboardScope;
}

export function Podium({ topUsers, scope }: PodiumProps) {
  const rank1 = topUsers.find((u) => u.rank === 1);
  const rank2 = topUsers.find((u) => u.rank === 2);
  const rank3 = topUsers.find((u) => u.rank === 3);

  return (
    <div className="relative pt-6 pb-2 px-2 sm:px-6">
      {/* Background ambient glow behind Top 1 */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-72 sm:w-96 h-72 sm:h-96 bg-amber-400/15 rounded-full blur-3xl pointer-events-none -z-10" />

      <div className="flex items-end justify-center gap-2 sm:gap-4 md:gap-6 max-w-4xl mx-auto">
        
        {/* RANK 2: SILVER (Left) */}
        {rank2 && (
          <div className="flex-1 flex flex-col items-center max-w-[240px] group">
            {/* Avatar & User Details */}
            <div className="relative mb-3 flex flex-col items-center">
              <div className="w-8 h-8 rounded-full bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-300 border border-slate-300 dark:border-slate-700 flex items-center justify-center mb-1 shadow-xs">
                <Medal className="w-4 h-4 text-slate-500 dark:text-slate-400" />
              </div>

              <div className="relative">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full ring-4 ring-slate-300 dark:ring-slate-600 bg-slate-200 dark:bg-slate-700 overflow-hidden shadow-md group-hover:scale-105 transition-transform duration-200">
                  {rank2.avatarUrl ? (
                    <img src={rank2.avatarUrl} alt={rank2.fullName} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center font-bold text-slate-600 dark:text-slate-200 text-lg sm:text-xl">
                      {rank2.fullName.charAt(0)}
                    </div>
                  )}
                </div>
                {/* Reward Badge / Honor Badge */}
                {scope === 'SUBJECT' ? (
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-2 py-0.5 bg-slate-900 text-slate-200 rounded-full text-[10px] sm:text-xs font-bold flex items-center gap-1 shadow-xs border border-slate-700 whitespace-nowrap">
                    <Medal className="w-3 h-3 text-slate-400" />
                    Á Khoa Môn
                  </div>
                ) : (
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-2 py-0.5 bg-slate-900 text-white rounded-full text-[10px] sm:text-xs font-bold flex items-center gap-1 shadow-xs border border-slate-700 whitespace-nowrap">
                    <Coins className="w-3 h-3 text-amber-400" />
                    +{getRewardCoins(2, scope)} xu
                  </div>
                )}
              </div>

              <div className="text-center mt-4">
                <h4 className="font-bold text-xs sm:text-sm text-slate-800 dark:text-slate-100 line-clamp-1">{rank2.fullName}</h4>
                <p className="text-[10px] sm:text-xs text-slate-500 dark:text-slate-400 font-mono mt-0.5">MSSV: {rank2.userCode}</p>
                <div className="flex items-center justify-center gap-1 mt-1 text-xs font-bold text-indigo-600 dark:text-indigo-400">
                  <Zap className="w-3 h-3" />
                  <span>{rank2.totalPoints.toLocaleString()} pts</span>
                </div>
              </div>
            </div>

            {/* Podium Pillar 2 */}
            <div className="w-full h-36 sm:h-44 rounded-t-2xl bg-linear-to-b from-slate-200 via-slate-100 to-slate-50 dark:from-slate-700 dark:via-slate-800 dark:to-slate-900 border-t-2 border-x-2 border-slate-300/80 dark:border-slate-700 shadow-sm flex flex-col items-center justify-between p-3">
              <span className="text-3xl sm:text-4xl font-extrabold text-slate-400/80 dark:text-slate-500 select-none">2</span>
              <span className="text-xs font-black text-slate-700 dark:text-slate-100 uppercase tracking-widest bg-slate-200/80 dark:bg-slate-800 px-2.5 py-1 rounded border border-slate-300/60 dark:border-slate-700">
                {scope === 'SUBJECT' ? 'Á Khoa' : 'Hạng Nhì'}
              </span>
            </div>
          </div>
        )}

        {/* RANK 1: GOLD (Center, Tallest) */}
        {rank1 && (
          <div className="flex-1 flex flex-col items-center max-w-[260px] z-10 group">
            {/* Crown & Avatar & User Details */}
            <div className="relative mb-3 flex flex-col items-center">
              <div className="relative flex items-center justify-center mb-1">
                <Crown className="w-8 h-8 sm:w-10 sm:h-10 text-amber-500 fill-amber-400 animate-bounce" />
                <Sparkles className="w-4 h-4 text-amber-400 absolute -top-1 -right-2 animate-pulse" />
              </div>

              <div className="relative">
                <div className="w-20 h-20 sm:w-26 sm:h-26 rounded-full ring-4 ring-amber-400 dark:ring-amber-500 bg-amber-100 dark:bg-amber-950/60 overflow-hidden shadow-xl shadow-amber-500/20 group-hover:scale-105 transition-transform duration-200">
                  {rank1.avatarUrl ? (
                    <img src={rank1.avatarUrl} alt={rank1.fullName} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center font-bold text-amber-700 dark:text-amber-300 text-2xl sm:text-3xl">
                      {rank1.fullName.charAt(0)}
                    </div>
                  )}
                </div>
                {/* Reward Badge Gold / Subject Top 1 Badge */}
                {scope === 'SUBJECT' ? (
                  <div className="absolute -bottom-2.5 left-1/2 -translate-x-1/2 px-2.5 py-0.5 bg-linear-to-r from-indigo-600 to-purple-600 text-white rounded-full text-xs font-extrabold flex items-center gap-1 shadow-md ring-2 ring-white dark:ring-slate-900 whitespace-nowrap">
                    <Sparkles className="w-3.5 h-3.5 text-amber-300 fill-amber-300" />
                    Thủ Khoa Môn
                  </div>
                ) : (
                  <div className="absolute -bottom-2.5 left-1/2 -translate-x-1/2 px-2.5 py-0.5 bg-linear-to-r from-amber-500 to-yellow-600 text-white rounded-full text-xs font-extrabold flex items-center gap-1 shadow-md ring-2 ring-white dark:ring-slate-900 whitespace-nowrap">
                    <Coins className="w-3.5 h-3.5 text-amber-100 fill-amber-200" />
                    +{getRewardCoins(1, scope)} xu
                  </div>
                )}
              </div>

              <div className="text-center mt-5">
                <h4 className="font-extrabold text-sm sm:text-base text-slate-900 dark:text-slate-100 line-clamp-1">{rank1.fullName}</h4>
                <p className="text-[10px] sm:text-xs text-amber-700 dark:text-amber-400 font-mono font-medium mt-0.5">MSSV: {rank1.userCode}</p>
                <div className="inline-flex items-center justify-center gap-1 mt-1 px-2.5 py-0.5 bg-amber-50 dark:bg-amber-950/60 border border-amber-200 dark:border-amber-700/60 rounded-full text-xs sm:text-sm font-black text-amber-700 dark:text-amber-300">
                  <Zap className="w-3.5 h-3.5 text-amber-600 fill-amber-500" />
                  <span>{rank1.totalPoints.toLocaleString()} pts</span>
                </div>
              </div>
            </div>

            {/* Podium Pillar 1 */}
            <div className="w-full h-48 sm:h-56 rounded-t-2xl bg-linear-to-b from-amber-300 via-yellow-100 to-amber-50 dark:from-amber-600/80 dark:via-amber-900/60 dark:to-slate-900 border-t-2 border-x-2 border-amber-400 dark:border-amber-500/60 shadow-md shadow-amber-500/10 flex flex-col items-center justify-between p-4">
              <div className="w-8 h-8 rounded-full bg-amber-400 dark:bg-amber-500 text-white flex items-center justify-center shadow-xs">
                <Trophy className="w-4 h-4 fill-white" />
              </div>
              <span className="text-4xl sm:text-5xl font-black text-amber-600/80 dark:text-amber-400 select-none">1</span>
              <span className="text-xs font-black text-amber-800 dark:text-amber-200 uppercase tracking-widest bg-amber-200/60 dark:bg-amber-950/80 px-2.5 py-1 rounded border border-amber-300/40 dark:border-amber-700/60">
                {scope === 'SUBJECT' ? 'Thủ Khoa' : 'Quán Quân'}
              </span>
            </div>
          </div>
        )}

        {/* RANK 3: BRONZE (Right) */}
        {rank3 && (
          <div className="flex-1 flex flex-col items-center max-w-[240px] group">
            {/* Avatar & User Details */}
            <div className="relative mb-3 flex flex-col items-center">
              <div className="w-8 h-8 rounded-full bg-orange-100 dark:bg-orange-950/60 text-amber-800 dark:text-amber-300 border border-orange-200 dark:border-orange-800/60 flex items-center justify-center mb-1 shadow-xs">
                <Award className="w-4 h-4 text-orange-600 dark:text-orange-400" />
              </div>

              <div className="relative">
                <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full ring-4 ring-orange-300 dark:ring-orange-600 bg-orange-100 dark:bg-orange-950 overflow-hidden shadow-md group-hover:scale-105 transition-transform duration-200">
                  {rank3.avatarUrl ? (
                    <img src={rank3.avatarUrl} alt={rank3.fullName} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center font-bold text-orange-700 dark:text-orange-300 text-lg sm:text-xl">
                      {rank3.fullName.charAt(0)}
                    </div>
                  )}
                </div>
                {/* Reward Badge / Honor Badge */}
                {scope === 'SUBJECT' ? (
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-2 py-0.5 bg-slate-900 text-orange-200 rounded-full text-[10px] sm:text-xs font-bold flex items-center gap-1 shadow-xs border border-slate-700 whitespace-nowrap">
                    <Award className="w-3 h-3 text-orange-400" />
                    Kiện Tướng
                  </div>
                ) : (
                  <div className="absolute -bottom-2 left-1/2 -translate-x-1/2 px-2 py-0.5 bg-slate-900 text-white rounded-full text-[10px] sm:text-xs font-bold flex items-center gap-1 shadow-xs border border-slate-700 whitespace-nowrap">
                    <Coins className="w-3 h-3 text-amber-400" />
                    +{getRewardCoins(3, scope)} xu
                  </div>
                )}
              </div>

              <div className="text-center mt-4">
                <h4 className="font-bold text-xs sm:text-sm text-slate-800 dark:text-slate-100 line-clamp-1">{rank3.fullName}</h4>
                <p className="text-[10px] sm:text-xs text-slate-500 dark:text-slate-400 font-mono mt-0.5">MSSV: {rank3.userCode}</p>
                <div className="flex items-center justify-center gap-1 mt-1 text-xs font-bold text-indigo-600 dark:text-indigo-400">
                  <Zap className="w-3 h-3" />
                  <span>{rank3.totalPoints.toLocaleString()} pts</span>
                </div>
              </div>
            </div>

            {/* Podium Pillar 3 */}
            <div className="w-full h-28 sm:h-36 rounded-t-2xl bg-linear-to-b from-orange-200 via-amber-100 to-amber-50/50 dark:from-orange-700/80 dark:via-orange-950/60 dark:to-slate-900 border-t-2 border-x-2 border-orange-300 dark:border-orange-700/60 shadow-sm flex flex-col items-center justify-between p-3">
              <span className="text-3xl sm:text-4xl font-extrabold text-orange-400/80 dark:text-orange-500 select-none">3</span>
              <span className="text-xs font-black text-orange-800 dark:text-orange-200 uppercase tracking-widest bg-orange-200/60 dark:bg-orange-950/80 px-2.5 py-1 rounded border border-orange-300/40 dark:border-orange-800/60">
                {scope === 'SUBJECT' ? 'Top 3 Môn' : 'Hạng Ba'}
              </span>
            </div>
          </div>
        )}

      </div>
    </div>
  );
}
