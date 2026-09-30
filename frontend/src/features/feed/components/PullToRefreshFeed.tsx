import React, { useState, useRef, useCallback } from 'react';
import { RefreshCw, Sparkles, ArrowDown } from 'lucide-react';

interface PullToRefreshFeedProps {
  onRefresh: () => Promise<void> | void;
  newCount?: number;
  children: React.ReactNode;
}

const PULL_THRESHOLD = 70; // px
const MAX_PULL = 110; // px

export const PullToRefreshFeed: React.FC<PullToRefreshFeedProps> = ({
  onRefresh,
  newCount = 0,
  children,
}) => {
  const [pullDistance, setPullDistance] = useState(0);
  const [isRefreshing, setIsRefreshing] = useState(false);
  const [isPulling, setIsPulling] = useState(false);

  const startYRef = useRef(0);
  const containerRef = useRef<HTMLDivElement>(null);

  const handleRefresh = useCallback(async () => {
    if (isRefreshing) return;
    setIsRefreshing(true);
    try {
      await onRefresh();
    } finally {
      setIsRefreshing(false);
      setPullDistance(0);
      setIsPulling(false);
    }
  }, [isRefreshing, onRefresh]);

  // Touch handlers for mobile pull-to-refresh
  const handleTouchStart = (e: React.TouchEvent) => {
    if (window.scrollY <= 0 && containerRef.current?.scrollTop === 0) {
      startYRef.current = e.touches[0].clientY;
      setIsPulling(true);
    }
  };

  const handleTouchMove = (e: React.TouchEvent) => {
    if (!isPulling || isRefreshing) return;
    const currentY = e.touches[0].clientY;
    const diff = currentY - startYRef.current;

    if (diff > 0 && window.scrollY <= 0) {
      // Damping resistance formula
      const damped = Math.min(Math.pow(diff, 0.82) * 1.5, MAX_PULL);
      setPullDistance(damped);
    } else {
      setPullDistance(0);
    }
  };

  const handleTouchEnd = async () => {
    if (!isPulling || isRefreshing) return;
    setIsPulling(false);

    if (pullDistance >= PULL_THRESHOLD) {
      await handleRefresh();
    } else {
      setPullDistance(0);
    }
  };

  // Scroll to top helper when user clicks the floating dynamic refresh button
  const handlePillClick = async () => {
    window.scrollTo({ top: 0, behavior: 'smooth' });
    await handleRefresh();
  };

  const pullProgress = Math.min(pullDistance / PULL_THRESHOLD, 1);
  const rotationDegrees = pullProgress * 360;

  return (
    <div
      ref={containerRef}
      onTouchStart={handleTouchStart}
      onTouchMove={handleTouchMove}
      onTouchEnd={handleTouchEnd}
      className="relative min-h-[400px]"
    >
      {/* 1. Pull Indicator Container (Gesture on pull down) */}
      <div
        className="overflow-hidden transition-all duration-200 flex items-center justify-center pointer-events-none"
        style={{
          height: isRefreshing ? '56px' : `${pullDistance}px`,
          opacity: pullDistance > 10 || isRefreshing ? 1 : 0,
        }}
      >
        <div
          className={`flex items-center gap-2 px-4 py-2 rounded-full shadow-md text-xs font-semibold backdrop-blur-md transition-colors ${
            pullProgress >= 1 || isRefreshing
              ? 'bg-blue-600 text-white shadow-blue-500/25 ring-2 ring-blue-400'
              : 'bg-white dark:bg-slate-800 text-slate-700 dark:text-slate-200 border border-slate-200 dark:border-slate-700'
          }`}
        >
          {isRefreshing ? (
            <>
              <RefreshCw className="w-4 h-4 animate-spin text-white" />
              <span>Đang cập nhật bảng tin...</span>
            </>
          ) : pullProgress >= 1 ? (
            <>
              <RefreshCw className="w-4 h-4 text-white animate-pulse" />
              <span>Thả tay để làm mới ngay</span>
            </>
          ) : (
            <>
              <ArrowDown
                className="w-4 h-4 transition-transform duration-150"
                style={{ transform: `rotate(${rotationDegrees}deg)` }}
              />
              <span>Kéo xuống để làm mới</span>
            </>
          )}
        </div>
      </div>

      {/* 2. Dynamic Floating Action Pill (When new posts arrive or on manual hover) */}
      <div className="sticky top-20 z-30 flex justify-center pointer-events-none mb-3">
        {newCount > 0 ? (
          <button
            type="button"
            onClick={handlePillClick}
            disabled={isRefreshing}
            className="pointer-events-auto group flex items-center gap-2.5 px-4 py-2 rounded-full bg-gradient-to-r from-blue-600 via-indigo-600 to-purple-600 text-white font-medium text-xs md:text-sm shadow-xl hover:shadow-blue-500/30 ring-2 ring-white dark:ring-slate-900 hover:scale-105 active:scale-95 transition-all duration-300 animate-bounce cursor-pointer"
          >
            <Sparkles className="w-4 h-4 text-amber-300 animate-spin" />
            <span>
              <strong>{newCount}</strong> bài viết mới
            </span>
            <span className="text-white/60">•</span>
            <span className="flex items-center gap-1 group-hover:underline">
              Bấm để làm mới
              <RefreshCw
                className={`w-3.5 h-3.5 transition-transform duration-300 ${
                  isRefreshing ? 'animate-spin' : 'group-hover:rotate-180'
                }`}
              />
            </span>
          </button>
        ) : null}
      </div>

      {/* 3. Feed Content Body */}
      {children}
    </div>
  );
};
