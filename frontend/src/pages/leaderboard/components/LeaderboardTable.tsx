import { useState } from 'react';
import type { LeaderboardEntryDto, MyRankDto } from '@/types/gamification.types';
import { Search, Zap, Trophy, Users } from 'lucide-react';

interface LeaderboardTableProps {
  entries: LeaderboardEntryDto[];
  myRank: MyRankDto;
  scopeTitle?: string;
}

export function LeaderboardTable({ entries, myRank, scopeTitle }: LeaderboardTableProps) {
  const [searchQuery, setSearchQuery] = useState('');

  const filteredEntries = entries.filter(
    (u) =>
      u.fullName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      u.userCode.toLowerCase().includes(searchQuery.toLowerCase())
  );

  // Tính khoảng cách điểm tới Top 3
  const top3Threshold = entries[2]?.totalPoints || 1200;
  const pointsToTop3 = Math.max(0, Math.round((top3Threshold - myRank.totalPoints + 1) * 10) / 10);

  return (
    <div className="space-y-4">
      {/* Search Bar */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 bg-white dark:bg-slate-900 p-4 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-xs">
        <div className="relative w-full sm:w-80">
          <Search className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4 h-4 text-slate-400 dark:text-slate-500" />
          <input
            type="text"
            placeholder="Tìm tên sinh viên, mã số sinh viên (MSSV)..."
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            className="w-full pl-10 pr-4 py-2 text-sm bg-slate-50 dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-xl focus:bg-white dark:focus:bg-slate-800 focus:border-indigo-500 text-slate-900 dark:text-slate-100 placeholder:text-slate-400 dark:placeholder:text-slate-500 focus:ring-2 focus:ring-indigo-500/10 outline-none transition-all"
          />
        </div>
        <div className="text-xs text-slate-500 dark:text-slate-400 font-medium">
          Hiển thị <span className="font-bold text-slate-800 dark:text-slate-200">{filteredEntries.length}</span> sinh viên
        </div>
      </div>

      {/* Sticky My Rank Card (Khai thác trọn vẹn MyRankDto từ Backend) */}
      <div className="bg-linear-to-r from-indigo-950 via-indigo-900 to-slate-900 text-white p-4 sm:p-5 rounded-2xl shadow-md flex flex-col sm:flex-row items-center justify-between gap-4 border border-indigo-700/80">
        <div className="flex items-center gap-3.5">
          <div className="w-12 h-12 rounded-xl bg-indigo-800/90 border border-indigo-500/50 flex flex-col items-center justify-center font-bold shrink-0">
            <span className="text-[10px] text-indigo-300 uppercase leading-none">Rank</span>
            <span className="text-lg text-white font-extrabold leading-tight">#{myRank.rank}</span>
          </div>

          <div>
            <div className="flex items-center gap-2">
              <span className="font-bold text-sm sm:text-base text-white">{myRank.fullName}</span>
              <span className="text-[10px] font-bold bg-indigo-500/40 text-indigo-200 px-2 py-0.5 rounded-full border border-indigo-400/30">
                Vị trí của bạn
              </span>
            </div>

            <div className="flex flex-wrap items-center gap-2 text-xs text-indigo-200 mt-1">
              <span className="text-amber-300 font-semibold flex items-center gap-1">
                <Trophy className="w-3.5 h-3.5" />
                Top {myRank.topPercent}% {scopeTitle || 'toàn trường'}
              </span>
              <span>•</span>
              <span className="flex items-center gap-1 text-slate-300">
                <Users className="w-3.5 h-3.5 text-indigo-400" />
                {myRank.totalParticipants.toLocaleString()} sinh viên {scopeTitle ? 'trong môn' : 'toàn trường'}
              </span>
            </div>

            {myRank.rank > 3 && pointsToTop3 > 0 && (
              <p className="text-[11px] text-indigo-300 mt-1">
                Cách Top 3 nhận thưởng: <span className="text-amber-300 font-bold">+{pointsToTop3} pts</span> nữa!
              </p>
            )}
          </div>
        </div>

        {/* Right Point Badge */}
        <div className="px-4 py-2 bg-indigo-800/60 rounded-xl border border-indigo-700/80 text-center shrink-0">
          <span className="block text-[10px] text-indigo-300 uppercase tracking-wider font-semibold">Điểm cống hiến</span>
          <span className="text-base sm:text-lg font-black text-amber-300 flex items-center justify-center gap-1">
            <Zap className="w-4 h-4 fill-amber-400 text-amber-400" />
            {myRank.totalPoints.toLocaleString()}
          </span>
        </div>
      </div>

      {/* Leaderboard Table List (From Rank 4 onward) */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 shadow-xs overflow-hidden">
        <div className="divide-y divide-slate-100 dark:divide-slate-800">
          {filteredEntries.map((user) => (
            <div
              key={user.userId}
              className={`p-3.5 sm:p-4 flex items-center justify-between gap-3 sm:gap-4 transition-colors ${
                user.isCurrentUser
                  ? 'bg-indigo-50/70 dark:bg-indigo-950/40 hover:bg-indigo-50 dark:hover:bg-indigo-900/40'
                  : 'hover:bg-slate-50/80 dark:hover:bg-slate-800/60'
              }`}
            >
              {/* Left: Rank & Avatar & Name */}
              <div className="flex items-center gap-3 sm:gap-4 min-w-0">
                {/* Rank Badge */}
                <div className="w-8 h-8 rounded-xl flex items-center justify-center font-bold text-sm shrink-0 bg-slate-100 dark:bg-slate-800 text-slate-700 dark:text-slate-300">
                  #{user.rank}
                </div>

                {/* Avatar with optional frame */}
                <div className="relative shrink-0">
                  <div className="w-10 h-10 rounded-full bg-slate-200 dark:bg-slate-700 overflow-hidden border border-slate-200 dark:border-slate-700 flex items-center justify-center font-bold text-slate-600 dark:text-slate-300 text-sm">
                    {user.avatarUrl ? (
                      <img src={user.avatarUrl} alt={user.fullName} className="w-full h-full object-cover" />
                    ) : (
                      user.fullName.charAt(0)
                    )}
                  </div>
                  {user.frameUrl && (
                    <img
                      src={user.frameUrl}
                      alt="Frame"
                      className="absolute -inset-1 w-[calc(100%+8px)] h-[calc(100%+8px)] object-contain pointer-events-none"
                    />
                  )}
                </div>

                {/* Name and MSSV */}
                <div className="min-w-0">
                  <div className="flex items-center gap-2">
                    <h5 className="font-bold text-sm text-slate-800 dark:text-slate-100 truncate">{user.fullName}</h5>
                    {user.isCurrentUser && (
                      <span className="text-[10px] font-bold bg-indigo-100 dark:bg-indigo-950/70 text-indigo-700 dark:text-indigo-300 px-1.5 py-0.2 rounded">
                        Bạn
                      </span>
                    )}
                  </div>
                  <p className="text-xs text-slate-400 dark:text-slate-500 font-mono">MSSV: {user.userCode}</p>
                </div>
              </div>

              {/* Right: Points */}
              <div className="px-3 py-1.5 bg-slate-50 dark:bg-slate-800 rounded-xl border border-slate-200/80 dark:border-slate-700 flex items-center gap-1.5 font-bold text-xs sm:text-sm text-indigo-600 dark:text-indigo-400 min-w-[90px] justify-end shrink-0">
                <Zap className="w-3.5 h-3.5 fill-indigo-600 dark:fill-indigo-400 shrink-0" />
                <span>{user.totalPoints.toLocaleString()}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
