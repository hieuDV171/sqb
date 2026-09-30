import React from 'react';
import { Search, Filter, BookOpen, Clock, Eye, CheckCircle2, Plus } from 'lucide-react';
import { cn } from '@/lib/utils';
import type { SubjectResponse, SessionStatus } from '../types/session.types';

interface SubmissionFilterBarProps {
  subjects: SubjectResponse[];
  selectedSubjectId?: number;
  onSelectSubject: (subjectId: number | undefined) => void;
  selectedStatus?: SessionStatus;
  onSelectStatus: (status: SessionStatus | undefined) => void;
  searchQuery: string;
  onSearchChange: (query: string) => void;
  onProposeNew?: () => void;
}

const STATUS_TABS: Array<{
  value?: SessionStatus;
  label: string;
  icon: React.ComponentType<{ className?: string }>;
  color: string;
}> = [
  { value: undefined, label: 'Tất cả', icon: Filter, color: 'text-slate-600 dark:text-slate-400' },
  { value: 'PENDING', label: 'Chờ duyệt', icon: Clock, color: 'text-amber-500' },
  { value: 'REVIEWING', label: 'Đang xem xét', icon: Eye, color: 'text-blue-500' },
  { value: 'RESOLVED', label: 'Đã duyệt', icon: CheckCircle2, color: 'text-emerald-500' },
];

export const SubmissionFilterBar: React.FC<SubmissionFilterBarProps> = ({
  subjects,
  selectedSubjectId,
  onSelectSubject,
  selectedStatus,
  onSelectStatus,
  searchQuery,
  onSearchChange,
  onProposeNew,
}) => {
  return (
    <div className="bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-3xl p-4 sm:p-5 shadow-xs backdrop-blur-md space-y-4">
      {/* Top row: Status Tabs & Action Button */}
      <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between gap-3">
        {/* Status Tabs */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-100/80 dark:bg-slate-800/80 rounded-2xl overflow-x-auto scrollbar-none">
          {STATUS_TABS.map((tab) => {
            const isSelected = selectedStatus === tab.value;
            const Icon = tab.icon;
            return (
              <button
                key={tab.label}
                type="button"
                onClick={() => onSelectStatus(tab.value)}
                className={cn(
                  'flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs sm:text-sm font-medium transition-all shrink-0 cursor-pointer',
                  isSelected
                    ? 'bg-white dark:bg-slate-900 text-slate-900 dark:text-white shadow-xs font-semibold'
                    : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-white/40 dark:hover:bg-slate-700/40'
                )}
              >
                <Icon className={cn('w-3.5 h-3.5', tab.color)} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>

        {/* Propose new shortcut button */}
        {onProposeNew && (
          <button
            type="button"
            onClick={onProposeNew}
            className="flex items-center justify-center gap-1.5 px-4 py-2 rounded-2xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs sm:text-sm font-semibold shadow-xs hover:shadow-indigo-200 transition-all cursor-pointer shrink-0"
          >
            <Plus className="w-4 h-4" />
            <span>Đề xuất phiên mới</span>
          </button>
        )}
      </div>

      {/* Bottom row: Subject Selector & Search Input */}
      <div className="grid grid-cols-1 sm:grid-cols-12 gap-3 pt-1">
        {/* Subject Select */}
        <div className="sm:col-span-5 relative">
          <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none text-slate-400">
            <BookOpen className="w-4 h-4" />
          </div>
          <select
            value={selectedSubjectId || ''}
            onChange={(e) => {
              const val = e.target.value ? Number(e.target.value) : undefined;
              onSelectSubject(val);
            }}
            className="w-full pl-9 pr-8 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/80 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all cursor-pointer"
          >
            <option value="">Tất cả môn học ({subjects.length})</option>
            {subjects.map((sub) => (
              <option key={sub.subjectId} value={sub.subjectId}>
                [{sub.code}] {sub.name}
              </option>
            ))}
          </select>
        </div>

        {/* Search Input */}
        <div className="sm:col-span-7 relative">
          <div className="absolute inset-y-0 left-3 flex items-center pointer-events-none text-slate-400">
            <Search className="w-4 h-4" />
          </div>
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => onSearchChange(e.target.value)}
            placeholder="Tìm theo tiêu đề, mã phiên..."
            className="w-full pl-9 pr-4 py-2 bg-slate-50 dark:bg-slate-800/60 border border-slate-200 dark:border-slate-700/80 rounded-2xl text-xs sm:text-sm text-slate-800 dark:text-slate-200 placeholder-slate-400 focus:outline-none focus:ring-2 focus:ring-indigo-500/20 focus:border-indigo-500 transition-all"
          />
        </div>
      </div>
    </div>
  );
};
