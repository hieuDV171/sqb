import React from 'react';

export const FeedCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-5 shadow-xs mb-4 animate-pulse">
      {/* Header Skeleton */}
      <div className="flex items-center gap-3 mb-4">
        <div className="w-11 h-11 rounded-full bg-slate-200 dark:bg-slate-800 shrink-0" />
        <div className="flex-1 space-y-2">
          <div className="w-36 h-4 rounded-md bg-slate-200 dark:bg-slate-800" />
          <div className="w-24 h-3 rounded-md bg-slate-100 dark:bg-slate-800/60" />
        </div>
        <div className="w-16 h-6 rounded-full bg-slate-100 dark:bg-slate-800" />
      </div>

      {/* Content Skeleton */}
      <div className="space-y-2 mb-4">
        <div className="w-full h-3.5 rounded-md bg-slate-200 dark:bg-slate-800" />
        <div className="w-5/6 h-3.5 rounded-md bg-slate-200 dark:bg-slate-800" />
        <div className="w-2/3 h-3.5 rounded-md bg-slate-100 dark:bg-slate-800/60" />
      </div>

      {/* Media Skeleton */}
      <div className="w-full h-48 rounded-2xl bg-slate-100 dark:bg-slate-800 mb-4" />

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
