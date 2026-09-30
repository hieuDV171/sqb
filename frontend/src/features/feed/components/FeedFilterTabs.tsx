import React from 'react';
import type { FeedFilter } from '../hooks/useActivityFeed';
import { LayoutGrid, MessageSquare, Award, Video, FileQuestion, RefreshCw } from 'lucide-react';

interface FeedFilterTabsProps {
  currentFilter: FeedFilter;
  onFilterChange: (filter: FeedFilter) => void;
  onManualRefresh?: () => void;
  isRefreshing?: boolean;
}

export const FeedFilterTabs: React.FC<FeedFilterTabsProps> = ({
  currentFilter,
  onFilterChange,
  onManualRefresh,
  isRefreshing,
}) => {
  const tabs: { key: FeedFilter; label: string; icon: React.ComponentType<{ className?: string }> }[] = [
    { key: 'ALL', label: 'Tất cả', icon: LayoutGrid },
    { key: 'POSTS', label: 'Bài viết', icon: MessageSquare },
    { key: 'SESSIONS', label: 'Phiên câu hỏi', icon: FileQuestion },
    { key: 'ACHIEVEMENTS', label: 'Huy hiệu & Điểm', icon: Award },
    { key: 'VIDEOS', label: 'Video học tập', icon: Video },
  ];

  return (
    <div className="flex items-center justify-between gap-2 overflow-x-auto pb-1 scrollbar-none mb-4">
      <div className="flex items-center gap-1.5 p-1 bg-slate-100 dark:bg-slate-800/80 rounded-2xl border border-slate-200/80 dark:border-slate-700/60 shadow-xs">
        {tabs.map((tab) => {
          const Icon = tab.icon;
          const isActive = currentFilter === tab.key;
          return (
            <button
              key={tab.key}
              type="button"
              onClick={() => onFilterChange(tab.key)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs md:text-sm font-medium transition-all duration-200 cursor-pointer whitespace-nowrap ${
                isActive
                  ? 'bg-white dark:bg-slate-900 text-blue-600 dark:text-blue-400 shadow-xs font-semibold'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-900 dark:hover:text-slate-200 hover:bg-white/50 dark:hover:bg-slate-700/50'
              }`}
            >
              <Icon className={`w-4 h-4 ${isActive ? 'text-blue-600 dark:text-blue-400' : 'text-slate-400'}`} />
              <span>{tab.label}</span>
            </button>
          );
        })}
      </div>

      {onManualRefresh && (
        <button
          type="button"
          onClick={onManualRefresh}
          disabled={isRefreshing}
          title="Làm mới bảng tin (Pull to refresh)"
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs md:text-sm font-semibold text-slate-600 dark:text-slate-300 hover:text-blue-600 dark:hover:text-blue-400 hover:bg-slate-100 dark:hover:bg-slate-800 border border-slate-200/80 dark:border-slate-700/60 transition-all cursor-pointer shrink-0 disabled:opacity-50 active:scale-95 shadow-xs"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${isRefreshing ? 'animate-spin text-blue-600 dark:text-blue-400' : ''}`} />
          <span className="hidden sm:inline">{isRefreshing ? 'Đang tải...' : 'Làm mới'}</span>
        </button>
      )}
    </div>
  );
};
