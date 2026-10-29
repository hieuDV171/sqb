import React from 'react';
import type { ReviewCourseClassOption } from '../types/review.types';
import { Search, Filter, ArrowUpDown } from 'lucide-react';

interface PendingSessionFilterBarProps {
  searchQuery: string;
  onSearchChange: (query: string) => void;
  selectedCourseClassId?: number;
  onCourseClassChange: (courseClassId?: number) => void;
  courseClasses: ReviewCourseClassOption[];
  sortBy: string;
  onSortChange: (sort: string) => void;
}

export const PendingSessionFilterBar: React.FC<PendingSessionFilterBarProps> = ({
  searchQuery,
  onSearchChange,
  selectedCourseClassId,
  onCourseClassChange,
  courseClasses,
  sortBy,
  onSortChange,
}) => {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/90 dark:border-slate-800 p-4 shadow-xs flex flex-col md:flex-row items-center justify-between gap-3">
      {/* Search Input */}
      <div className="relative w-full md:w-80">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={searchQuery}
          onChange={(e) => onSearchChange(e.target.value)}
          placeholder="Tìm theo tiêu đề hoặc mã phiên..."
          className="w-full pl-9 pr-4 py-2 bg-slate-50 dark:bg-slate-800/60 rounded-2xl text-xs md:text-sm text-slate-800 dark:text-slate-100 placeholder:text-slate-400 border border-slate-200 dark:border-slate-700 focus:outline-hidden focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
        />
      </div>

      {/* Selectors */}
      <div className="flex items-center gap-2.5 w-full md:w-auto flex-wrap">
        {/* Assigned Course Class Filter */}
        <div className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs font-medium text-slate-700 dark:text-slate-300 flex-1 md:min-w-72">
          <Filter className="w-3.5 h-3.5 text-indigo-500 shrink-0" />
          <select
            aria-label="Lọc theo lớp học phần"
            value={selectedCourseClassId || ''}
            onChange={(e) =>
              onCourseClassChange(e.target.value ? Number(e.target.value) : undefined)
            }
            className="bg-transparent focus:outline-hidden cursor-pointer w-full"
          >
            <option value="">Tất cả lớp học phần</option>
            {courseClasses.map((courseClass) => (
              <option key={courseClass.courseClassId} value={courseClass.courseClassId}>
                [{courseClass.subjectCode}] {courseClass.classCode} · HK {courseClass.semesterName}
              </option>
            ))}
          </select>
        </div>

        {/* Sort Filter */}
        <div className="flex items-center gap-1.5 px-3 py-2 rounded-2xl bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700 text-xs font-medium text-slate-700 dark:text-slate-300">
          <ArrowUpDown className="w-3.5 h-3.5 text-slate-400 shrink-0" />
          <select
            value={sortBy}
            onChange={(e) => onSortChange(e.target.value)}
            className="bg-transparent focus:outline-hidden cursor-pointer"
          >
            <option value="createdAt,desc">Mới nhất trước</option>
            <option value="createdAt,asc">Cũ nhất trước</option>
          </select>
        </div>
      </div>
    </div>
  );
};
