import type { SubjectItem } from '@/types/post.types';
import { Link } from 'react-router-dom';
import { BookOpen, Trophy, Zap, ExternalLink, Target, Users } from 'lucide-react';
import { useLeaderboard } from '@/features/gamification/hooks/useGamification';

interface FeedSidebarRightProps {
  subjects: SubjectItem[];
  selectedSubjectId?: number;
  onSelectSubject: (subjectId?: number) => void;
}

export function FeedSidebarRight({
  subjects,
  selectedSubjectId,
  onSelectSubject,
}: FeedSidebarRightProps) {
  const { data: leaderboardData, isLoading, isError } = useLeaderboard({
    period: 'SEMESTER',
    limit: 3,
  });
  const topEntries = leaderboardData?.entries?.slice(0, 3) ?? [];
  const myRank = leaderboardData?.myRank;

  return (
    <aside className="space-y-4 w-full">
      
      {/* Widget 1: Real enrolled-subject filter */}
      <div className="bg-white dark:bg-slate-900 rounded-2xl border border-slate-200/80 dark:border-slate-800 p-4 shadow-xs space-y-3 transition-colors">
        <div className="flex items-center justify-between">
          <h3 className="text-sm font-bold text-slate-900 dark:text-slate-100 flex items-center gap-1.5">
            <BookOpen className="w-4 h-4 text-indigo-600 dark:text-indigo-400" />
            Lọc theo môn học
          </h3>
          {selectedSubjectId && (
            <button
              type="button"
              onClick={() => onSelectSubject(undefined)}
              className="text-[11px] font-semibold text-indigo-600 dark:text-indigo-400 hover:text-indigo-800 dark:hover:text-indigo-300 cursor-pointer"
            >
              Xóa bộ lọc
            </button>
          )}
        </div>

        <div className="space-y-1">
          {subjects.length === 0 && (
            <div className="rounded-xl bg-slate-50 p-3 text-[11px] leading-relaxed text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
              Chưa có lớp học phần nào gắn với tài khoản để lọc bảng tin.
            </div>
          )}
          {subjects.map((sub) => {
            const isSelected = selectedSubjectId === sub.id;

            return (
              <button
                key={sub.id}
                type="button"
                onClick={() => onSelectSubject(isSelected ? undefined : sub.id)}
                className={`w-full flex items-center justify-between p-2 rounded-xl text-left text-xs transition-colors cursor-pointer ${
                  isSelected
                    ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-700 dark:text-indigo-400 font-bold border border-indigo-200 dark:border-indigo-800'
                    : 'text-slate-700 dark:text-slate-300 hover:bg-slate-50 dark:hover:bg-slate-800'
                }`}
              >
                <div className="flex items-center gap-2 min-w-0">
                  <BookOpen className={`w-3.5 h-3.5 shrink-0 ${isSelected ? 'text-indigo-600 dark:text-indigo-400' : 'text-slate-400 dark:text-slate-500'}`} />
                  <span className="truncate">{sub.name}</span>
                </div>
                <span className="shrink-0 pl-2 font-mono text-[10px] text-slate-400 dark:text-slate-500">
                  {sub.code}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Widget 2: Mini Leaderboard Preview */}
      <div className="bg-linear-to-br from-indigo-950 via-slate-900 to-slate-950 text-white rounded-2xl p-4 shadow-sm space-y-3 border border-indigo-800/60 dark:border-slate-800">
        <div className="flex items-center justify-between">
          <h3 className="text-xs font-bold text-amber-300 flex items-center gap-1.5 uppercase tracking-wider">
            <Trophy className="w-3.5 h-3.5 text-amber-400 fill-amber-400" />
            Bảng Vàng Học Kỳ
          </h3>
          <Link
            to="/leaderboard"
            className="text-[11px] font-semibold text-indigo-300 hover:text-white flex items-center gap-0.5"
          >
            Chi tiết <ExternalLink className="w-3 h-3" />
          </Link>
        </div>

        <div className="space-y-2" aria-live="polite">
          {isLoading && Array.from({ length: 3 }).map((_, index) => (
            <div key={index} className="h-9 animate-pulse rounded-xl bg-white/10" />
          ))}
          {isError && (
            <div className="rounded-xl bg-white/10 p-3 text-center text-[11px] text-indigo-100">
              Chưa thể tải bảng xếp hạng.
            </div>
          )}
          {!isLoading && !isError && topEntries.length === 0 && (
            <div className="rounded-xl bg-white/10 p-3 text-center text-[11px] text-indigo-100">
              Chưa có dữ liệu xếp hạng học kỳ.
            </div>
          )}
          {topEntries.map((entry) => (
            <Link
              key={entry.userId}
              to={`/users/${entry.userId}`}
              className="flex min-h-9 items-center justify-between rounded-xl bg-white/8 p-2 text-xs transition-colors hover:bg-white/15"
            >
              <div className="flex min-w-0 items-center gap-2">
                <span className={`flex h-5 w-5 shrink-0 items-center justify-center rounded-full text-[10px] font-black text-slate-950 ${
                  entry.rank === 1 ? 'bg-amber-400' : entry.rank === 2 ? 'bg-slate-300' : 'bg-orange-400'
                }`}>
                  {entry.rank}
                </span>
                <span className="truncate font-semibold text-slate-100">{entry.fullName}</span>
              </div>
              <span className="ml-2 flex shrink-0 items-center gap-0.5 text-[11px] font-bold text-indigo-200">
                <Zap className="h-3 w-3" /> {entry.totalPoints.toLocaleString()}
              </span>
            </Link>
          ))}
        </div>

        <p className="text-[10px] text-indigo-200/80 leading-relaxed text-center pt-1 border-t border-white/10">
          Cuộc đua Top 3 nhận <strong>+300 / +180 / +90 xu</strong> cuối kỳ!
        </p>
      </div>

      {/* Widget 3: Real current-user rank */}
      <div className="space-y-3 rounded-2xl border border-slate-200/80 bg-white p-4 shadow-xs transition-colors dark:border-slate-800 dark:bg-slate-900">
        <div className="flex items-center gap-1.5 text-xs font-bold text-slate-900 dark:text-slate-100">
          <Target className="h-4 w-4 text-indigo-600 dark:text-indigo-400" />
          <span>Vị trí của bạn</span>
        </div>
        {isLoading ? (
          <div className="h-16 animate-pulse rounded-xl bg-slate-100 dark:bg-slate-800" />
        ) : myRank && myRank.rank > 0 ? (
          <div className="flex items-center justify-between rounded-xl bg-indigo-50 p-3 dark:bg-indigo-950/40">
            <div>
              <p className="text-lg font-black text-indigo-700 dark:text-indigo-300">#{myRank.rank}</p>
              <p className="flex items-center gap-1 text-[10px] text-slate-500 dark:text-slate-400">
                <Users className="h-3 w-3" /> {myRank.totalParticipants.toLocaleString()} người tham gia
              </p>
            </div>
            <div className="text-right">
              <p className="text-sm font-black text-slate-900 dark:text-white">
                {myRank.totalPoints.toLocaleString()} điểm
              </p>
              <p className="text-[10px] font-semibold text-indigo-600 dark:text-indigo-400">
                Top {myRank.topPercent}% học kỳ
              </p>
            </div>
          </div>
        ) : (
          <div className="rounded-xl bg-slate-50 p-3 text-[11px] leading-relaxed text-slate-500 dark:bg-slate-800/60 dark:text-slate-400">
            Bạn chưa có thứ hạng. Hãy đóng góp câu hỏi hoặc luyện tập để bắt đầu tích điểm.
          </div>
        )}
        <Link
          to="/leaderboard"
          className="inline-flex min-h-9 w-full items-center justify-center rounded-xl border border-slate-200 text-xs font-semibold text-slate-700 transition-colors hover:border-indigo-300 hover:text-indigo-600 dark:border-slate-700 dark:text-slate-300 dark:hover:text-indigo-400"
        >
          Xem bảng xếp hạng
        </Link>
      </div>

    </aside>
  );
}
