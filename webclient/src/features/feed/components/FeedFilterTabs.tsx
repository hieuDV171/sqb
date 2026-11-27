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
    <div className="mb-4 flex min-w-0 items-center justify-between gap-2 pb-1" aria-label="Bộ lọc bảng tin">
      <div className="min-w-0 overflow-x-auto scrollbar-none">
        <div className="flex w-max items-center gap-1.5 rounded-2xl border border-border bg-muted/70 p-1 shadow-[var(--shadow-primitive-xs)]" role="tablist">
          {tabs.map((tab) => {
            const Icon = tab.icon;
            const isActive = currentFilter === tab.key;
            return (
              <button
                key={tab.key}
                type="button"
                role="tab"
                aria-selected={isActive}
                onClick={() => onFilterChange(tab.key)}
                className={`flex min-h-11 items-center gap-1.5 whitespace-nowrap rounded-xl px-3 text-sm font-medium transition-colors duration-150 ${
                  isActive
                    ? 'bg-card text-primary shadow-[var(--shadow-primitive-xs)] font-semibold'
                    : 'text-muted-foreground hover:bg-card/60 hover:text-foreground'
                }`}
              >
                <Icon className={`size-4 ${isActive ? 'text-primary' : 'text-muted-foreground'}`} />
                <span>{tab.label}</span>
              </button>
            );
          })}
        </div>
      </div>

      {onManualRefresh && (
        <button
          type="button"
          onClick={onManualRefresh}
          disabled={isRefreshing}
          aria-label="Làm mới bảng tin"
          aria-busy={isRefreshing}
          title="Làm mới bảng tin"
          className="flex min-h-11 min-w-11 shrink-0 cursor-pointer items-center justify-center gap-1.5 rounded-xl border border-border bg-card px-3 text-sm font-semibold text-muted-foreground shadow-[var(--shadow-primitive-xs)] transition-colors hover:text-primary focus-visible:outline-2 focus-visible:outline-ring disabled:cursor-wait disabled:opacity-50"
        >
          <RefreshCw aria-hidden="true" className={`size-3.5 ${isRefreshing ? 'motion-safe:animate-spin text-primary' : ''}`} />
          <span className="hidden sm:inline">{isRefreshing ? 'Đang tải...' : 'Làm mới'}</span>
        </button>
      )}
    </div>
  );
};
