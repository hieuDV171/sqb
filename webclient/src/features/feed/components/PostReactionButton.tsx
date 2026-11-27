import { useEffect, useId, useRef, useState, type KeyboardEvent, type MouseEvent, type PointerEvent } from 'react';
import { Popover } from '@base-ui/react/popover';
import { Heart, Star, ThumbsUp } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { ReactionType } from '../services/reactService';

const openDelay = 450;
const pressMoveLimit = 10;
const reactions = {
  LIKE: {
    label: 'Thích', icon: ThumbsUp, color: 'text-blue-600 dark:text-blue-400',
    background: 'bg-blue-50 dark:bg-blue-950/40', ring: 'ring-blue-300 dark:ring-blue-800',
  },
  LOVE: {
    label: 'Yêu thích', icon: Heart, color: 'text-rose-600 dark:text-rose-400',
    background: 'bg-rose-50 dark:bg-rose-950/40', ring: 'ring-rose-300 dark:ring-rose-800',
  },
  WOW: {
    label: 'Tuyệt vời', icon: Star, color: 'text-amber-700 dark:text-amber-400',
    background: 'bg-amber-50 dark:bg-amber-950/40', ring: 'ring-amber-300 dark:ring-amber-800',
  },
} satisfies Record<ReactionType, { label: string; icon: typeof ThumbsUp; color: string; background: string; ring: string }>;
const reactionTypes: ReactionType[] = ['LIKE', 'LOVE', 'WOW'];

type Press = { id: number; x: number; y: number; timer: number; pointerType: string };

interface PostReactionButtonProps {
  reaction: ReactionType | null;
  count: number;
  busy: boolean;
  onToggle: (reaction: ReactionType) => void;
}

export function PostReactionButton({ reaction, count, busy, onToggle }: PostReactionButtonProps) {
  const triggerId = useId();
  const [openMode, setOpenMode] = useState<'hover' | 'press' | 'keyboard' | null>(null);
  const press = useRef<Press | null>(null);
  const suppressPointerClick = useRef(false);
  const suppressHoverUntilLeave = useRef(false);
  const firstOption = useRef<HTMLButtonElement>(null);
  const selected = reaction ? reactions[reaction] : undefined;
  const ActiveIcon = selected?.icon ?? ThumbsUp;

  useEffect(() => () => window.clearTimeout(press.current?.timer), []);

  function endPress(event: PointerEvent<HTMLButtonElement>) {
    if (press.current?.id !== event.pointerId) return;
    window.clearTimeout(press.current.timer);
    press.current = null;
  }

  function startPress(event: PointerEvent<HTMLButtonElement>) {
    if (!event.isPrimary || event.button !== 0 || busy) return;
    window.clearTimeout(press.current?.timer);
    suppressPointerClick.current = false;
    press.current = {
      id: event.pointerId,
      x: event.clientX,
      y: event.clientY,
      pointerType: event.pointerType,
      timer: window.setTimeout(() => {
        suppressPointerClick.current = true;
        setOpenMode('press');
      }, openDelay),
    };
  }

  function trackPress(event: PointerEvent<HTMLButtonElement>) {
    const start = press.current;
    if (start?.id === event.pointerId && Math.hypot(event.clientX - start.x, event.clientY - start.y) > pressMoveLimit) {
      suppressPointerClick.current = true;
      endPress(event);
    }
  }

  function toggle(type: ReactionType) {
    if (busy) return;
    setOpenMode(null);
    onToggle(type);
  }

  function clickTrigger(event: MouseEvent<HTMLButtonElement>) {
    if (event.detail !== 0 && suppressPointerClick.current) {
      suppressPointerClick.current = false;
      return;
    }
    if (event.detail !== 0) suppressHoverUntilLeave.current = true;
    toggle(reaction ?? 'LIKE');
  }

  function handleTriggerKey(event: KeyboardEvent<HTMLButtonElement>) {
    if (busy || (event.key !== 'ArrowUp' && event.key !== 'ArrowDown')) return;
    event.preventDefault();
    setOpenMode('keyboard');
    firstOption.current?.focus();
  }

  function changeOpen(open: boolean, details: Popover.Root.ChangeEventDetails) {
    const ignoreHover = press.current || openMode === 'press' || openMode === 'keyboard' || (open && suppressHoverUntilLeave.current);
    if (details.reason === 'trigger-press' || (details.reason === 'trigger-hover' && ignoreHover)) {
      details.cancel();
      return;
    }
    setOpenMode(open ? 'hover' : null);
  }

  return (
    <Popover.Root open={openMode !== null} triggerId={triggerId} onOpenChange={changeOpen}>
      <Popover.Trigger
        id={triggerId}
        render={<Button type="button" variant="ghost" disabled={busy} focusableWhenDisabled
          aria-busy={busy || undefined} className={cn('min-h-11 touch-manipulation gap-1.5 px-3 [-webkit-touch-callout:none]', selected?.background)} />}
        aria-label={selected ? `Bỏ cảm xúc ${selected.label}` : 'Thích'}
        aria-keyshortcuts="ArrowUp ArrowDown"
        aria-description="Nhấn mũi tên lên hoặc xuống để chọn cảm xúc."
        openOnHover={!busy && openMode !== 'press' && openMode !== 'keyboard'}
        delay={openDelay}
        closeDelay={160}
        onClick={clickTrigger}
        onKeyDown={handleTriggerKey}
        onPointerDown={startPress}
        onPointerMove={trackPress}
        onPointerUp={endPress}
        onPointerCancel={endPress}
        onPointerLeave={(event) => {
          suppressHoverUntilLeave.current = false;
          endPress(event);
        }}
        onContextMenu={(event) => {
          if (suppressPointerClick.current || (press.current && press.current.pointerType !== 'mouse')) event.preventDefault();
        }}
      >
        <ActiveIcon aria-hidden="true" className={selected?.color} />
        <span className={selected?.color}>{selected?.label ?? 'Thích'}</span>
        {count > 0 && <span className="text-xs font-semibold">{count}</span>}
      </Popover.Trigger>
      <Popover.Portal>
        <Popover.Positioner side="top" align="center" sideOffset={6} collisionPadding={8} className="z-50">
          <Popover.Popup aria-label="Cảm xúc bài viết" initialFocus={openMode === 'keyboard'}
            className="flex origin-(--transform-origin) gap-1 rounded-xl border border-border bg-popover p-1.5 text-popover-foreground shadow-lg motion-safe:transition-[opacity,scale] motion-safe:duration-150 motion-safe:data-starting-style:scale-[0.97] data-starting-style:opacity-0">
            {reactionTypes.map((type, index) => {
              const { label, icon: Icon, color, background, ring } = reactions[type];
              const isSelected = reaction === type;
              return (
                <Button key={type} ref={index === 0 ? firstOption : undefined} type="button" size="icon" variant="ghost"
                  aria-label={label} aria-pressed={isSelected} disabled={busy} onClick={() => toggle(type)}
                  className={cn('size-11 cursor-pointer motion-safe:[&_svg]:transition-transform motion-safe:[&_svg]:duration-150 motion-safe:hover:[&_svg]:-translate-y-0.5 motion-safe:hover:[&_svg]:scale-115',
                    isSelected && cn(background, 'ring-1 ring-inset', ring))}>
                  <Icon aria-hidden="true" className={cn('size-[1.125rem]', color)} />
                </Button>
              );
            })}
          </Popover.Popup>
        </Popover.Positioner>
      </Popover.Portal>
    </Popover.Root>
  );
}
