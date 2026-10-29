import React, { useState, useEffect, useMemo } from 'react';
import { useReviewableSessions } from '@/features/review/hooks/useReviewableSessions';
import { PendingSessionCard } from '@/features/review/components/PendingSessionCard';
import { PendingSessionFilterBar } from '@/features/review/components/PendingSessionFilterBar';
import { PendingSessionSkeleton } from '@/features/review/components/PendingSessionSkeleton';
import { PendingReviewsEmpty } from '@/features/review/components/PendingReviewsEmpty';
import { ErrorState } from '@/components/ui/error-state';
import { axiosClient } from '@/api/axiosClient';
import type { ReviewCourseClassOption } from '@/features/review/types/review.types';
import type { GlobalResponse } from '@/types/response.types';
import { useAuthStore } from '@/stores/useAuthStore';
import { ClipboardCheck, Loader2 } from 'lucide-react';

export const PendingReviewsPage: React.FC = () => {
  const { user } = useAuthStore();
  const [selectedCourseClassId, setSelectedCourseClassId] = useState<number | undefined>(undefined);
  const [sortBy, setSortBy] = useState('createdAt,desc');
  const [searchQuery, setSearchQuery] = useState('');
  const [courseClasses, setCourseClasses] = useState<ReviewCourseClassOption[]>([]);

  // 1. Fetch course classes available to the current reviewer
  useEffect(() => {
    const loadCourseClasses = async () => {
      try {
        setCourseClasses([]);
        if (user?.role === 'LECTURER') {
          const classRes = (await axiosClient.get<GlobalResponse<ReviewCourseClassOption[]>>(
            '/lecturer/course-classes/my-classes',
            { params: { include_history: true } }
          )) as unknown as GlobalResponse<ReviewCourseClassOption[]>;
          if (Array.isArray(classRes.data)) {
            setCourseClasses(classRes.data);
            return;
          }
        }

        if (user?.role === 'ADMIN') {
          const adminRes = (await axiosClient.get<GlobalResponse<ReviewCourseClassOption[]>>(
            '/lecturer/course-classes'
          )) as unknown as GlobalResponse<ReviewCourseClassOption[]>;
          if (Array.isArray(adminRes.data)) {
            setCourseClasses(adminRes.data);
          }
        }
      } catch (err) {
        console.warn('Lỗi khi tải danh sách lớp học phần:', err);
      }
    };

    loadCourseClasses();
  }, [user?.role]);

  // 2. Fetch sessions that are reviewable by the current lecturer
  const {
    data,
    isLoading,
    isError,
    refetch,
    hasNextPage,
    fetchNextPage,
    isFetchingNextPage,
  } = useReviewableSessions({
    course_class_id: selectedCourseClassId,
    sort_by: sortBy,
    limit: 10,
  });

  const allItems = useMemo(() => {
    if (!data?.pages) return [];
    return data.pages.flatMap((page) => page?.items || []);
  }, [data]);

  // Local search filter
  const displayedItems = useMemo(() => {
    if (!searchQuery.trim()) return allItems;
    const q = searchQuery.toLowerCase().trim();
    return allItems.filter(
      (item) =>
        item.title?.toLowerCase().includes(q) ||
        item.topic?.toLowerCase().includes(q) ||
        item.subjectCode?.toLowerCase().includes(q) ||
        item.classCode?.toLowerCase().includes(q) ||
        item.semesterName?.toLowerCase().includes(q) ||
        item.author?.fullName?.toLowerCase().includes(q) ||
        String(item.sessionId).includes(q)
    );
  }, [allItems, searchQuery]);

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Page Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <div className="flex items-center gap-2.5">
            <div className="p-2 rounded-2xl bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400">
              <ClipboardCheck className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-xl md:text-2xl font-bold tracking-tight text-slate-900 dark:text-slate-100">
                Hàng Đợi Duyệt Đề Xuất
              </h1>
              <p className="text-xs md:text-sm text-slate-500 dark:text-slate-400 mt-0.5">
                Xem xét, đánh giá và kiểm tra trùng lặp các câu hỏi do sinh viên đóng góp
              </p>
            </div>
          </div>
        </div>

        {allItems.length > 0 && (
          <div className="flex items-center gap-2 px-3 py-1.5 rounded-2xl bg-indigo-50 dark:bg-indigo-950/50 border border-indigo-200 dark:border-indigo-800 text-xs font-semibold text-indigo-700 dark:text-indigo-300 self-start sm:self-auto">
            <span>{allItems.length} phiên đang chờ đánh giá</span>
          </div>
        )}
      </div>

      {/* Filter Bar */}
      <PendingSessionFilterBar
        searchQuery={searchQuery}
        onSearchChange={setSearchQuery}
        selectedCourseClassId={selectedCourseClassId}
        onCourseClassChange={setSelectedCourseClassId}
        courseClasses={courseClasses}
        sortBy={sortBy}
        onSortChange={setSortBy}
      />

      {/* 4 UI States */}
      {isLoading ? (
        <div className="space-y-4">
          <PendingSessionSkeleton />
          <PendingSessionSkeleton />
          <PendingSessionSkeleton />
        </div>
      ) : isError ? (
        <ErrorState
          title="Không thể tải danh sách phiên cần kiểm duyệt"
          message="Đã có lỗi xảy ra khi kết nối máy chủ. Vui lòng thử lại sau."
          onRetry={() => refetch()}
        />
      ) : displayedItems.length === 0 ? (
        <PendingReviewsEmpty />
      ) : (
        <div className="space-y-4">
          {displayedItems.map((session) => (
            <PendingSessionCard key={session.sessionId} session={session} />
          ))}

          {/* Load More Button */}
          {hasNextPage && (
            <div className="text-center pt-4">
              <button
                type="button"
                onClick={() => fetchNextPage()}
                disabled={isFetchingNextPage}
                className="inline-flex items-center gap-2 px-5 py-2.5 rounded-2xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 hover:bg-slate-50 dark:hover:bg-slate-800/80 text-xs font-semibold text-slate-700 dark:text-slate-300 shadow-xs cursor-pointer disabled:opacity-50"
              >
                {isFetchingNextPage ? (
                  <>
                    <Loader2 className="w-4 h-4 animate-spin" />
                    <span>Đang tải thêm phiên nộp...</span>
                  </>
                ) : (
                  <span>Tải thêm phiên cần kiểm duyệt</span>
                )}
              </button>
            </div>
          )}
        </div>
      )}
    </div>
  );
};
