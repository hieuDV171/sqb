import { cn } from '@/lib/utils';

export interface AvatarWithFrameProps {
  avatarUrl?: string | null;
  frameUrl?: string | null;
  name?: string;
  size?: 'xs' | 'sm' | 'md' | 'lg' | 'xl' | '2xl';
  showOnlineStatus?: boolean;
  isOnline?: boolean;
  className?: string;
}

const SIZE_MAP = {
  xs: {
    container: 'w-7 h-7 text-[10px]',
    frameOffset: '-inset-1',
    frameSize: 'w-[calc(100%+8px)] h-[calc(100%+8px)]',
    indicator: 'w-2 h-2 bottom-0 right-0',
  },
  sm: {
    container: 'w-9 h-9 text-xs',
    frameOffset: '-inset-1.5',
    frameSize: 'w-[calc(100%+12px)] h-[calc(100%+12px)]',
    indicator: 'w-2.5 h-2.5 bottom-0 right-0',
  },
  md: {
    container: 'w-12 h-12 text-sm',
    frameOffset: '-inset-2',
    frameSize: 'w-[calc(100%+16px)] h-[calc(100%+16px)]',
    indicator: 'w-3 h-3 bottom-0.5 right-0.5',
  },
  lg: {
    container: 'w-16 h-16 text-base',
    frameOffset: '-inset-2.5',
    frameSize: 'w-[calc(100%+20px)] h-[calc(100%+20px)]',
    indicator: 'w-3.5 h-3.5 bottom-0.5 right-0.5',
  },
  xl: {
    container: 'w-24 h-24 text-xl',
    frameOffset: '-inset-3.5',
    frameSize: 'w-[calc(100%+28px)] h-[calc(100%+28px)]',
    indicator: 'w-4 h-4 bottom-1 right-1',
  },
  '2xl': {
    container: 'w-32 h-32 text-3xl',
    frameOffset: '-inset-4',
    frameSize: 'w-[calc(100%+32px)] h-[calc(100%+32px)]',
    indicator: 'w-5 h-5 bottom-1.5 right-1.5',
  },
};

export function AvatarWithFrame({
  avatarUrl,
  frameUrl,
  name = 'Người dùng',
  size = 'md',
  showOnlineStatus = false,
  isOnline = true,
  className,
}: AvatarWithFrameProps) {
  const currentSize = SIZE_MAP[size] || SIZE_MAP.md;
  const initial = name.trim().charAt(0).toUpperCase() || 'U';

  return (
    <div className={cn('relative inline-flex shrink-0 select-none items-center justify-center', className)}>
      {/* Base Avatar Circle */}
      <div
        className={cn(
          'rounded-full overflow-hidden flex items-center justify-center font-bold text-white shadow-xs bg-linear-to-tr from-indigo-500 via-purple-500 to-pink-500 ring-2 ring-white dark:ring-slate-900',
          currentSize.container
        )}
      >
        {avatarUrl ? (
          <img
            src={avatarUrl}
            alt={name}
            className="w-full h-full object-cover transition-transform duration-300 hover:scale-105"
            onError={(e) => {
              // Fallback to initial on broken image
              (e.target as HTMLElement).style.display = 'none';
            }}
          />
        ) : (
          <span>{initial}</span>
        )}
      </div>

      {/* Cosmetic Avatar Frame Overlay */}
      {frameUrl && (
        <img
          src={frameUrl}
          alt="Avatar Frame"
          className={cn(
            'absolute object-contain pointer-events-none z-10 drop-shadow-md',
            currentSize.frameOffset,
            currentSize.frameSize
          )}
        />
      )}

      {/* Online/Offline Status Indicator */}
      {showOnlineStatus && (
        <span
          className={cn(
            'absolute rounded-full ring-2 ring-white dark:ring-slate-900 z-20',
            currentSize.indicator,
            isOnline ? 'bg-emerald-500 animate-pulse' : 'bg-slate-400'
          )}
        />
      )}
    </div>
  );
}
