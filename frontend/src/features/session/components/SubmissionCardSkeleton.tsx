import React from 'react';

export const SubmissionCardSkeleton: React.FC = () => {
  return (
    <div className="bg-white/80 dark:bg-slate-900/80 border border-slate-200/80 dark:border-slate-800/80 rounded-2xl p-5 shadow-xs space-y-4 animate-pulse backdrop-blur-xs">
      <div className="flex items-center justify-between gap-3">
        <div className="flex items-center gap-2">
          <div className="h-6 w-20 bg-slate-200 dark:bg-slate-800 rounded-full" />
          <div className="h-5 w-28 bg-slate-200 dark:bg-slate-800 rounded-md font-mono" />
        </div>
        <div className="h-6 w-24 bg-slate-200 dark:bg-slate-800 rounded-full" />
      </div>

      <div className="space-y-2">
        <div className="h-5 w-3/4 bg-slate-200 dark:bg-slate-800 rounded-md" />
        <div className="h-4 w-full bg-slate-100 dark:bg-slate-800/60 rounded-md" />
        <div className="h-4 w-2/3 bg-slate-100 dark:bg-slate-800/60 rounded-md" />
      </div>

      <div className="pt-3 border-t border-slate-100 dark:border-slate-800/80 flex items-center justify-between">
        <div className="flex items-center gap-4">
          <div className="h-4 w-16 bg-slate-200 dark:bg-slate-800 rounded-md" />
          <div className="h-4 w-12 bg-slate-200 dark:bg-slate-800 rounded-md" />
          <div className="h-4 w-24 bg-slate-200 dark:bg-slate-800 rounded-md" />
        </div>
        <div className="h-8 w-24 bg-slate-200 dark:bg-slate-800 rounded-xl" />
      </div>
    </div>
  );
};
