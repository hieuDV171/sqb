import { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { Trophy, Search, ChevronDown, Check } from 'lucide-react';
import { useLeaderboard } from '../hooks/useGamification';
import type { LeaderboardPeriod } from '../types/gamification.types';
import { sessionService } from '@/features/session/services/sessionService';

interface SubjectItem {
  id: number;
  code: string;
  name: string;
}

export function LeaderboardTab() {
  const [period, setPeriod] = useState<LeaderboardPeriod>('SEMESTER');
  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [subjects, setSubjects] = useState<SubjectItem[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [isSubjectDropdownOpen, setIsSubjectDropdownOpen] = useState(false);

  // Fetch subjects for SUBJECT scope filter (dùng danh mục môn học GET /subjects chung cho mọi người dùng)
  useEffect(() => {
    let isMounted = true;
    const loadSubjects = async () => {
      try {
        const res: any = await sessionService.getAllSubjects();
        const rawList = Array.isArray(res) ? res : res?.data;
        const list = Array.isArray(rawList) ? rawList : [];
        if (isMounted) {
          const mapped = list.map((s: any) => ({
            id: s.id || s.subjectId,
            code: s.code || s.subjectCode,
            name: s.name || s.subjectName,
          }));
          setSubjects(mapped);
          if (mapped.length > 0) {
            setSelectedSubjectId((prev) => prev ?? mapped[0].id);
          }
        }
      } catch (err) {
        if (isMounted) {
          setSubjects([]);
        }
      }
    };
    loadSubjects();
    return () => {
      isMounted = false;
    };
  }, []);

  const { data: leaderboardData, isLoading } = useLeaderboard({
    period,
    subjectId: period === 'SUBJECT' ? selectedSubjectId : undefined,
    limit: 50,
  });

  const entries = leaderboardData?.entries || [];
  const myRank = leaderboardData?.myRank;

  const top1 = entries.find((e) => e.rank === 1);
  const top2 = entries.find((e) => e.rank === 2);
  const top3 = entries.find((e) => e.rank === 3);

  const filteredEntries = entries.filter((entry) => {
    if (!searchQuery.trim()) return true;
    const q = searchQuery.toLowerCase().trim();
    return (
      entry.fullName.toLowerCase().includes(q) ||
      entry.userCode.toLowerCase().includes(q)
    );
  });

  const selectedSubject = subjects.find((s) => s.id === selectedSubjectId);

  return (
    <div className="space-y-8 animate-in fade-in duration-300">
      {/* Scope Filter Controls */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-4 p-4 rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs">
        {/* Period Buttons (Semester vs Subject) */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-900 rounded-xl">
          <button
            type="button"
            onClick={() => {
              setPeriod('SEMESTER');
              setSelectedSubjectId(undefined);
            }}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              period === 'SEMESTER'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Toàn Học Kỳ
          </button>
          <button
            type="button"
            onClick={() => {
              setPeriod('SUBJECT');
              if (!selectedSubjectId && subjects.length > 0) {
                setSelectedSubjectId(subjects[0].id);
              }
            }}
            className={`px-4 py-2 rounded-lg text-xs font-bold transition-all cursor-pointer ${
              period === 'SUBJECT'
                ? 'bg-white dark:bg-slate-800 text-indigo-600 dark:text-indigo-400 shadow-xs'
                : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-white'
            }`}
          >
            Theo Môn Học
          </button>
        </div>

        {/* Subject Picker Dropdown (when SUBJECT is active) */}
        {period === 'SUBJECT' && (
          <div className="relative flex-1 max-w-xs">
            <button
              type="button"
              onClick={() => setIsSubjectDropdownOpen(!isSubjectDropdownOpen)}
              className="w-full px-3.5 py-2 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs font-semibold flex items-center justify-between gap-2 text-slate-800 dark:text-slate-200 cursor-pointer"
            >
              <span className="truncate">
                {selectedSubject ? `${selectedSubject.code} - ${selectedSubject.name}` : 'Chọn môn học...'}
              </span>
              <ChevronDown className="w-4 h-4 text-slate-400 shrink-0" />
            </button>

            {isSubjectDropdownOpen && (
              <div className="absolute top-full left-0 right-0 mt-1 z-30 max-h-56 overflow-y-auto rounded-xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-700 shadow-xl p-1 space-y-0.5">
                {subjects.length === 0 ? (
                  <div className="px-3 py-3 text-center text-xs text-slate-400">
                    Chưa có môn học nào trong hệ thống
                  </div>
                ) : (
                  subjects.map((sub) => (
                    <button
                      key={sub.id}
                      type="button"
                      onClick={() => {
                        setSelectedSubjectId(sub.id);
                        setIsSubjectDropdownOpen(false);
                      }}
                      className={`w-full text-left px-3 py-2 rounded-lg text-xs font-medium flex items-center justify-between transition-colors ${
                        selectedSubjectId === sub.id
                          ? 'bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 font-bold'
                          : 'hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-700 dark:text-slate-300'
                      }`}
                    >
                      <span className="truncate">
                        <span className="font-mono font-bold">{sub.code}</span> - {sub.name}
                      </span>
                      {selectedSubjectId === sub.id && <Check className="w-3.5 h-3.5" />}
                    </button>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {/* Search Input in Table */}
        <div className="relative flex-1 max-w-xs">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tên hoặc MSSV..."
            className="w-full pl-9 pr-3.5 py-2 rounded-xl bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 text-xs text-slate-800 dark:text-slate-200 placeholder:text-slate-400 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20"
          />
        </div>
      </div>

      {/* Top 3 Podium (Hạng 1, 2, 3 3D-styled) */}
      {!isLoading && entries.length > 0 && (
        <div className="pt-8 pb-4">
          <div className="grid grid-cols-3 gap-2 sm:gap-6 items-end max-w-2xl mx-auto">
            {/* Top 2 - Silver (Left) */}
            <div className="flex flex-col items-center">
              {top2 ? (
                <div className="flex flex-col items-center text-center space-y-2 w-full animate-in fade-in slide-in-from-bottom-4 duration-500">
                  <div className="relative">
                    <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full border-4 border-slate-300 dark:border-slate-600 overflow-hidden shadow-lg bg-slate-200 dark:bg-slate-700">
                      {top2.avatarUrl ? (
                        <img src={top2.avatarUrl} alt={top2.fullName} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center font-bold text-slate-600 dark:text-slate-300">
                          {top2.fullName.charAt(0)}
                        </div>
                      )}
                    </div>
                    {top2.frameUrl && (
                      <img src={top2.frameUrl} alt="Frame" className="absolute inset-0 w-full h-full object-contain pointer-events-none scale-115" />
                    )}
                    <span className="absolute -top-2 -right-1 w-6 h-6 rounded-full bg-slate-400 text-white font-black text-xs flex items-center justify-center shadow-xs">
                      2
                    </span>
                  </div>

                  <Link
                    to={`/users/${top2.userId}`}
                    className="text-xs sm:text-sm font-bold text-slate-900 dark:text-white truncate max-w-[110px] sm:max-w-[140px] hover:text-indigo-600 dark:hover:text-indigo-400"
                  >
                    {top2.fullName}
                  </Link>
                  <p className="text-[11px] font-mono font-semibold text-slate-500">
                    {top2.totalPoints.toLocaleString()} XP
                  </p>
                </div>
              ) : null}

              {/* Pedestal Top 2 */}
              <div className="w-full h-24 sm:h-32 mt-3 rounded-t-2xl bg-linear-to-b from-slate-200 to-slate-300 dark:from-slate-700 dark:to-slate-800 border-t-4 border-slate-400/80 shadow-inner flex flex-col items-center justify-center text-slate-600 dark:text-slate-300">
                <span className="text-xl sm:text-2xl font-black">2</span>
                <span className="text-[10px] uppercase font-bold tracking-wider opacity-70">Á Quân</span>
              </div>
            </div>

            {/* Top 1 - Gold (Center, Tallest) */}
            <div className="flex flex-col items-center">
              {top1 ? (
                <div className="flex flex-col items-center text-center space-y-2 w-full animate-in fade-in slide-in-from-bottom-6 duration-700">
                  <span className="text-2xl animate-bounce">👑</span>
                  <div className="relative">
                    <div className="w-20 h-20 sm:w-26 sm:h-26 rounded-full border-4 border-amber-400 overflow-hidden shadow-xl ring-4 ring-amber-400/30 bg-amber-100 dark:bg-amber-950/60">
                      {top1.avatarUrl ? (
                        <img src={top1.avatarUrl} alt={top1.fullName} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center font-bold text-amber-700 dark:text-amber-300 text-xl">
                          {top1.fullName.charAt(0)}
                        </div>
                      )}
                    </div>
                    {top1.frameUrl && (
                      <img src={top1.frameUrl} alt="Frame" className="absolute inset-0 w-full h-full object-contain pointer-events-none scale-115" />
                    )}
                    <span className="absolute -top-2 -right-1 w-7 h-7 rounded-full bg-linear-to-r from-amber-400 to-yellow-500 text-amber-950 font-black text-sm flex items-center justify-center shadow-md">
                      1
                    </span>
                  </div>

                  <Link
                    to={`/users/${top1.userId}`}
                    className="text-xs sm:text-base font-black text-slate-900 dark:text-white truncate max-w-[120px] sm:max-w-[160px] hover:text-amber-600 dark:hover:text-amber-400"
                  >
                    {top1.fullName}
                  </Link>
                  <p className="text-xs sm:text-sm font-mono font-black text-amber-500">
                    {top1.totalPoints.toLocaleString()} XP
                  </p>
                </div>
              ) : null}

              {/* Pedestal Top 1 */}
              <div className="w-full h-32 sm:h-44 mt-3 rounded-t-2xl bg-linear-to-b from-amber-200 to-yellow-300 dark:from-amber-900/60 dark:to-yellow-950/60 border-t-4 border-amber-400 shadow-inner flex flex-col items-center justify-center text-amber-900 dark:text-amber-300">
                <span className="text-2xl sm:text-3xl font-black">1</span>
                <span className="text-[10px] uppercase font-bold tracking-wider opacity-80">Quán Quân</span>
              </div>
            </div>

            {/* Top 3 - Bronze (Right) */}
            <div className="flex flex-col items-center">
              {top3 ? (
                <div className="flex flex-col items-center text-center space-y-2 w-full animate-in fade-in slide-in-from-bottom-3 duration-400">
                  <div className="relative">
                    <div className="w-16 h-16 sm:w-20 sm:h-20 rounded-full border-4 border-amber-700/60 overflow-hidden shadow-lg bg-amber-100/50 dark:bg-amber-950/40">
                      {top3.avatarUrl ? (
                        <img src={top3.avatarUrl} alt={top3.fullName} className="w-full h-full object-cover" />
                      ) : (
                        <div className="w-full h-full flex items-center justify-center font-bold text-amber-800 dark:text-amber-400">
                          {top3.fullName.charAt(0)}
                        </div>
                      )}
                    </div>
                    {top3.frameUrl && (
                      <img src={top3.frameUrl} alt="Frame" className="absolute inset-0 w-full h-full object-contain pointer-events-none scale-115" />
                    )}
                    <span className="absolute -top-2 -right-1 w-6 h-6 rounded-full bg-amber-700 text-white font-black text-xs flex items-center justify-center shadow-xs">
                      3
                    </span>
                  </div>

                  <Link
                    to={`/users/${top3.userId}`}
                    className="text-xs sm:text-sm font-bold text-slate-900 dark:text-white truncate max-w-[110px] sm:max-w-[140px] hover:text-indigo-600 dark:hover:text-indigo-400"
                  >
                    {top3.fullName}
                  </Link>
                  <p className="text-[11px] font-mono font-semibold text-slate-500">
                    {top3.totalPoints.toLocaleString()} XP
                  </p>
                </div>
              ) : null}

              {/* Pedestal Top 3 */}
              <div className="w-full h-20 sm:h-28 mt-3 rounded-t-2xl bg-linear-to-b from-amber-100 to-amber-200 dark:from-amber-950/50 dark:to-amber-900/40 border-t-4 border-amber-700/60 shadow-inner flex flex-col items-center justify-center text-amber-800 dark:text-amber-400">
                <span className="text-xl sm:text-2xl font-black">3</span>
                <span className="text-[10px] uppercase font-bold tracking-wider opacity-70">Quý Quân</span>
              </div>
            </div>
          </div>
        </div>
      )}

      {/* Sticky My Rank Bar (if available) */}
      {myRank && myRank.rank > 0 && (
        <div className="p-4 rounded-2xl bg-linear-to-r from-indigo-500/10 via-purple-500/10 to-pink-500/10 border-2 border-indigo-500/30 flex items-center justify-between gap-4 shadow-sm">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600 text-white font-black flex items-center justify-center text-sm shadow-xs">
              #{myRank.rank}
            </div>
            <div>
              <p className="text-xs font-bold text-slate-900 dark:text-white flex items-center gap-1.5">
                <span>Vị trí của bạn trên bảng vinh danh</span>
                <span className="px-2 py-0.5 rounded-md text-[10px] font-bold bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
                  Top {myRank.topPercent}%
                </span>
              </p>
              <p className="text-[11px] text-slate-500 dark:text-slate-400">
                Tổng số người tham gia: {myRank.totalParticipants.toLocaleString()} sinh viên
              </p>
            </div>
          </div>

          <div className="text-right">
            <p className="text-xs text-slate-500 uppercase font-semibold">Điểm tích lũy</p>
            <p className="text-lg font-black font-mono text-indigo-600 dark:text-indigo-400">
              {myRank.totalPoints.toLocaleString()} điểm
            </p>
          </div>
        </div>
      )}

      {/* Main Leaderboard Table */}
      <div className="overflow-hidden rounded-2xl bg-white dark:bg-slate-800/80 border border-slate-200/80 dark:border-slate-700 shadow-xs">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs sm:text-sm">
            <thead>
              <tr className="border-b border-slate-200 dark:border-slate-700 bg-slate-50 dark:bg-slate-900/50 text-slate-500 dark:text-slate-400 uppercase tracking-wider text-[11px] font-bold">
                <th className="py-3.5 px-4 w-16 text-center">Hạng</th>
                <th className="py-3.5 px-4">Sinh Viên</th>
                <th className="py-3.5 px-4 hidden sm:table-cell">Mã Số SV</th>
                <th className="py-3.5 px-4 text-right">Tổng Điểm</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-100 dark:divide-slate-700/60">
              {isLoading ? (
                Array.from({ length: 6 }).map((_, i) => (
                  <tr key={i} className="animate-pulse">
                    <td className="py-4 px-4 text-center">
                      <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-700 mx-auto" />
                    </td>
                    <td className="py-4 px-4">
                      <div className="flex items-center gap-3">
                        <div className="w-9 h-9 rounded-full bg-slate-200 dark:bg-slate-700" />
                        <div className="space-y-1.5">
                          <div className="w-28 h-3.5 bg-slate-200 dark:bg-slate-700 rounded-sm" />
                          <div className="w-16 h-3 bg-slate-200 dark:bg-slate-700 rounded-sm" />
                        </div>
                      </div>
                    </td>
                    <td className="py-4 px-4 hidden sm:table-cell">
                      <div className="w-20 h-3.5 bg-slate-200 dark:bg-slate-700 rounded-sm" />
                    </td>
                    <td className="py-4 px-4 text-right">
                      <div className="w-16 h-4 bg-slate-200 dark:bg-slate-700 rounded-sm ml-auto" />
                    </td>
                  </tr>
                ))
              ) : filteredEntries.length === 0 ? (
                <tr>
                  <td colSpan={4} className="py-12 text-center text-slate-400">
                    <Trophy className="w-10 h-10 mx-auto mb-2 opacity-30" />
                    {period === 'SUBJECT' && !selectedSubjectId ? (
                      <>
                        <p className="font-semibold text-sm">
                          {subjects.length === 0
                            ? 'Chưa tìm thấy môn học nào gắn với tài khoản của bạn'
                            : 'Vui lòng chọn môn học để xem Bảng xếp hạng'}
                        </p>
                        <p className="text-xs text-slate-500 mt-1">
                          {subjects.length === 0
                            ? 'Bảng xếp hạng theo môn học chỉ hiển thị khi tài khoản có môn học theo học hoặc giảng dạy.'
                            : 'Chọn một môn từ menu thả xuống bên trên để tải danh sách vinh danh.'}
                        </p>
                      </>
                    ) : (
                      <>
                        <p className="font-semibold text-sm">Chưa có dữ liệu xếp hạng</p>
                        <p className="text-xs text-slate-500 mt-1">
                          Hãy tham gia đóng góp câu hỏi và luyện tập để xuất hiện tại đây!
                        </p>
                      </>
                    )}
                  </td>
                </tr>
              ) : (
                filteredEntries.map((entry) => {
                  const isTop1 = entry.rank === 1;
                  const isTop2 = entry.rank === 2;
                  const isTop3 = entry.rank === 3;

                  return (
                    <tr
                      key={entry.userId}
                      className={`transition-colors ${
                        entry.isCurrentUser
                          ? 'bg-indigo-50/80 dark:bg-indigo-950/40 font-semibold'
                          : 'hover:bg-slate-50/80 dark:hover:bg-slate-750'
                      }`}
                    >
                      {/* Rank Icon / Number */}
                      <td className="py-3.5 px-4 text-center font-bold">
                        {isTop1 ? (
                          <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-amber-400 text-amber-950 font-black text-xs shadow-xs">
                            🥇
                          </span>
                        ) : isTop2 ? (
                          <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-slate-300 dark:bg-slate-600 text-slate-800 dark:text-white font-black text-xs shadow-xs">
                            🥈
                          </span>
                        ) : isTop3 ? (
                          <span className="inline-flex items-center justify-center w-7 h-7 rounded-full bg-amber-700 text-white font-black text-xs shadow-xs">
                            🥉
                          </span>
                        ) : (
                          <span className="text-slate-500 font-mono">#{entry.rank}</span>
                        )}
                      </td>

                      {/* User Info */}
                      <td className="py-3.5 px-4">
                        <Link
                          to={`/users/${entry.userId}`}
                          className="flex items-center gap-3 group"
                        >
                          <div className="relative shrink-0">
                            <div className="w-9 h-9 rounded-full overflow-hidden bg-slate-200 dark:bg-slate-700 flex items-center justify-center shadow-xs">
                              {entry.avatarUrl ? (
                                <img
                                  src={entry.avatarUrl}
                                  alt={entry.fullName}
                                  className="w-full h-full object-cover"
                                />
                              ) : (
                                <span className="font-bold text-slate-600 dark:text-slate-300 text-xs">
                                  {entry.fullName.charAt(0)}
                                </span>
                              )}
                            </div>
                            {entry.frameUrl && (
                              <img
                                src={entry.frameUrl}
                                alt="Frame"
                                className="absolute inset-0 w-full h-full object-contain pointer-events-none scale-115"
                              />
                            )}
                          </div>

                          <div>
                            <p className="font-bold text-slate-900 dark:text-white group-hover:text-indigo-600 dark:group-hover:text-indigo-400 transition-colors">
                              {entry.fullName}
                              {entry.isCurrentUser && (
                                <span className="ml-2 px-1.5 py-0.5 rounded-sm text-[10px] font-bold bg-indigo-100 dark:bg-indigo-900/60 text-indigo-600 dark:text-indigo-400">
                                  Bạn
                                </span>
                              )}
                            </p>
                            <p className="text-[11px] text-slate-400 font-mono sm:hidden">
                              {entry.userCode}
                            </p>
                          </div>
                        </Link>
                      </td>

                      {/* Student Code */}
                      <td className="py-3.5 px-4 hidden sm:table-cell font-mono text-slate-500 dark:text-slate-400">
                        {entry.userCode}
                      </td>

                      {/* Points */}
                      <td className="py-3.5 px-4 text-right">
                        <span className="font-mono font-black text-indigo-600 dark:text-indigo-400 text-sm">
                          {entry.totalPoints.toLocaleString()}
                        </span>
                        <span className="text-[10px] text-slate-400 ml-1">điểm</span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}
