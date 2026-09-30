import React from 'react';

export const PendingSessionSkeleton: React.FC = () => {
  return (
    <div className="bg-white dark:bg-slate-900 rounded-3xl border border-slate-200/80 dark:border-slate-800 p-5 md:p-6 shadow-xs animate-pulse space-y-4">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <div className="h-6 w-24 bg-slate-200 dark:bg-slate-800 rounded-xl" />
          <div className="h-4 w-12 bg-slate-100 dark:bg-slate-800 rounded-md" />
        </div>
        <div className="h-6 w-20 bg-slate-200 dark:bg-slate-800 rounded-full" />
      </div>

      <div className="space-y-2">
        <div className="h-5 w-3/4 bg-slate-200 dark:bg-slate-800 rounded-lg" />
        <div className="h-4 w-full bg-slate-100 dark:bg-slate-800 rounded-lg" />
      </div>

      <div className="flex items-center justify-between pt-3 border-t border-slate-100 dark:border-slate-800">
        <div className="flex items-center gap-3">
          <div className="w-6 h-6 rounded-full bg-slate-200 dark:bg-slate-800" />
          <div className="h-4 w-28 bg-slate-100 dark:bg-slate-800 rounded-md" />
        </div>
        <div className="h-8 w-28 bg-slate-200 dark:bg-slate-800 rounded-2xl" />
      </div>
    </div>
  );
};
