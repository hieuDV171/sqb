import React, { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { BookOpen, ArrowLeft, Filter, Search } from 'lucide-react';
import { useUserProposedSessions, UserSessionCard } from '@/features/practice';
import { useEnrolledSubjects } from '@/features/session';
import { Skeleton } from '@/components/ui/skeleton';
import { ErrorState } from '@/components/ui/error-state';

export const UserSessionsPage: React.FC = () => {
  const { userId } = useParams<{ userId: string }>();
  const navigate = useNavigate();
  const numericUserId = Number(userId);

  const [selectedSubjectId, setSelectedSubjectId] = useState<number | undefined>(undefined);
  const [searchQuery, setSearchQuery] = useState('');

  const { data: subjects = [] } = useEnrolledSubjects();
  const {
    data: sessionsData,
    isLoading,
    isError,
    error,
    refetch,
    hasNextPage,
    isFetchingNextPage,
    fetchNextPage,
  } = useUserProposedSessions(numericUserId, {
    subject_id: selectedSubjectId,
  });

  const allSessions = useMemo(() => {
    if (!sessionsData?.pages) return [];
    return sessionsData.pages.flatMap((page) => page?.contents || []);
  }, [sessionsData]);

  const filteredSessions = useMemo(() => {
    if (!searchQuery.trim()) return allSessions;
    const q = searchQuery.toLowerCase().trim();
    return allSessions.filter(
      (s) =>
        s.title.toLowerCase().includes(q) ||
        (s.sessionCode && s.sessionCode.toLowerCase().includes(q)) ||
        (s.subjectName && s.subjectName.toLowerCase().includes(q))
    );
  }, [allSessions, searchQuery]);

  return (
    <div className="max-w-5xl mx-auto px-3 sm:px-6 py-6 sm:py-8 space-y-6">
      {/* Top Header */}
      <div className="border-b border-slate-200/80 dark:border-slate-800 pb-5">
        <button
          type="button"
          onClick={() => navigate(-1)}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-800 dark:hover:text-slate-200 mb-2 transition-colors cursor-pointer"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Quay lại</span>
        </button>

        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <span className="p-2.5 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <BookOpen className="w-6 h-6" />
            </span>
            <div>
              <h1 className="text-xl sm:text-2xl font-black text-slate-900 dark:text-white tracking-tight">
                Bộ đề câu hỏi của thành viên
              </h1>
              <p className="text-xs sm:text-sm text-slate-500 dark:text-slate-400">
                Khám phá và luyện tập các bộ câu hỏi trắc nghiệm đã được phê duyệt
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* Filter and Search Bar */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Subject Filter */}
        <div className="flex items-center gap-2">
          <Filter className="w-4 h-4 text-slate-400 shrink-0" />
          <select
            value={selectedSubjectId || ''}
            onChange={(e) =>
              setSelectedSubjectId(e.target.value ? Number(e.target.value) : undefined)
            }
            className="bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl px-3 py-2 text-xs font-semibold text-slate-700 dark:text-slate-200 focus:outline-hidden focus:border-indigo-500 transition-colors"
          >
            <option value="">Tất cả môn học</option>
            {subjects.map((sub) => (
              <option key={sub.subjectId} value={sub.subjectId}>
                {sub.code} - {sub.name}
              </option>
            ))}
          </select>
        </div>

        {/* Search Input */}
        <div className="relative flex-1 sm:max-w-xs">
          <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Tìm theo tiêu đề hoặc mã phiên..."
            className="w-full pl-9 pr-3.5 py-2 bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 rounded-xl text-xs text-slate-800 dark:text-slate-100 placeholder:text-slate-400 focus:outline-hidden focus:border-indigo-500 transition-all"
          />
        </div>
      </div>

      {/* Content State Handling */}
      {isLoading ? (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <div
              key={i}
              className="p-5 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-4"
            >
              <div className="flex justify-between items-center">
                <Skeleton className="h-5 w-20 rounded-md" />
                <Skeleton className="h-8 w-20 rounded-xl" />
              </div>
              <Skeleton className="h-6 w-3/4 rounded-md" />
              <Skeleton className="h-4 w-full rounded-md" />
              <div className="flex justify-between items-center pt-2">
                <Skeleton className="h-4 w-28 rounded-md" />
                <Skeleton className="h-4 w-20 rounded-md" />
              </div>
            </div>
          ))}
        </div>
      ) : isError ? (
        <ErrorState
          title="Không thể tải danh sách bộ câu hỏi"
          message={(error as Error)?.message || 'Vui lòng thử lại sau.'}
          onRetry={() => refetch()}
        />
      ) : filteredSessions.length === 0 ? (
        <div className="text-center py-16 p-8 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 space-y-3">
          <div className="w-14 h-14 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-500 mx-auto flex items-center justify-center">
            <BookOpen className="w-7 h-7" />
          </div>
          <h3 className="font-bold text-slate-800 dark:text-slate-200 text-base">
            Không tìm thấy bộ câu hỏi nào
          </h3>
          <p className="text-xs text-slate-400 max-w-sm mx-auto">
            Người dùng này hiện chưa có phiên câu hỏi nào được phê duyệt hoặc không khớp với bộ lọc.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {filteredSessions.map((session) => (
              <UserSessionCard key={session.sessionId} session={session} />
            ))}
          </div>

          {/* Load More Button */}
          {hasNextPage && (
            <div className="text-center pt-4">
              <button
                type="button"
                onClick={() => fetchNextPage()}
                disabled={isFetchingNextPage}
                className="px-6 py-2.5 rounded-xl bg-slate-100 dark:bg-slate-800 hover:bg-slate-200 dark:hover:bg-slate-700 text-xs sm:text-sm font-bold text-slate-700 dark:text-slate-200 transition-all cursor-pointer disabled:opacity-50"
              >
                {isFetchingNextPage ? 'Đang tải thêm...' : 'Tải thêm bộ đề'}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
