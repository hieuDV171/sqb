import React from 'react';

export const FeedCardSkeleton: React.FC = () => {
  return (
    <div className="mb-4 animate-pulse rounded-2xl border border-border bg-card p-5 shadow-[var(--card-shadow)]" aria-hidden="true">
      {/* Header Skeleton */}
      <div className="flex items-center gap-3 mb-4">
        <div className="size-11 shrink-0 rounded-full bg-muted" />
        <div className="flex-1 space-y-2">
          <div className="h-4 w-36 rounded-md bg-muted" />
          <div className="h-3 w-24 rounded-md bg-muted/70" />
        </div>
        <div className="h-6 w-16 rounded-full bg-muted" />
      </div>

      {/* Content Skeleton */}
      <div className="space-y-2 mb-4">
        <div className="h-3.5 w-full rounded-md bg-muted" />
        <div className="h-3.5 w-5/6 rounded-md bg-muted" />
        <div className="h-3.5 w-2/3 rounded-md bg-muted/70" />
      </div>

      {/* Media Skeleton */}
      <div className="mb-4 h-48 w-full rounded-2xl bg-muted" />

      {/* Footer Skeleton */}
      <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-12 h-6 rounded-xl bg-slate-100 dark:bg-slate-800" />
          <div className="w-16 h-6 rounded-xl bg-slate-100 dark:bg-slate-800" />
        </div>
        <div className="w-8 h-6 rounded-xl bg-slate-100 dark:bg-slate-800" />
      </div>
    </div>
  );
};
