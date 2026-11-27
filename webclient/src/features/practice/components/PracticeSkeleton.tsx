import React from 'react';
import { Skeleton } from '@/components/ui/skeleton';

export const PracticeSkeleton: React.FC = () => {
  return (
    <div className="max-w-4xl mx-auto space-y-6 px-3 sm:px-6 py-6 sm:py-8">
      {/* Top Header & Breadcrumb Skeleton */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <Skeleton className="h-5 w-32 rounded-lg" />
          <Skeleton className="h-6 w-24 rounded-full" />
        </div>
        <Skeleton className="h-8 w-3/4 rounded-xl" />
        <Skeleton className="h-4 w-1/2 rounded-md" />
      </div>

      {/* Progress Palette Skeleton */}
      <div className="p-4 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-xs space-y-3">
        <div className="flex justify-between items-center">
          <Skeleton className="h-4 w-36 rounded-md" />
          <Skeleton className="h-4 w-20 rounded-md" />
        </div>
        <Skeleton className="h-2.5 w-full rounded-full" />
        <div className="flex flex-wrap gap-2 pt-2">
          {Array.from({ length: 8 }).map((_, i) => (
            <Skeleton key={i} className="w-9 h-9 rounded-xl" />
          ))}
        </div>
      </div>

      {/* Question Card Skeleton */}
      <div className="p-6 sm:p-8 rounded-3xl bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 shadow-md space-y-6">
        <div className="flex justify-between items-center">
          <Skeleton className="h-6 w-28 rounded-full" />
          <div className="flex gap-2">
            <Skeleton className="w-8 h-8 rounded-xl" />
            <Skeleton className="w-8 h-8 rounded-xl" />
            <Skeleton className="w-8 h-8 rounded-xl" />
          </div>
        </div>

        {/* Content text */}
        <div className="space-y-2.5">
          <Skeleton className="h-5 w-full rounded-md" />
          <Skeleton className="h-5 w-5/6 rounded-md" />
          <Skeleton className="h-5 w-2/3 rounded-md" />
        </div>

        {/* Options */}
        <div className="space-y-3 pt-2">
          {Array.from({ length: 4 }).map((_, i) => (
            <Skeleton key={i} className="h-14 w-full rounded-2xl" />
          ))}
        </div>

        {/* Action Button */}
        <div className="flex justify-between items-center pt-4 border-t border-slate-100 dark:border-slate-800">
          <Skeleton className="h-10 w-24 rounded-xl" />
          <Skeleton className="h-10 w-36 rounded-xl" />
        </div>
      </div>
    </div>
  );
};
