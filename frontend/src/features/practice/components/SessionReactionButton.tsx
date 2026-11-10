import { Heart } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { cn } from '@/lib/utils';
import type { ReactionType } from '@/features/feed/services/reactService';
import { useSessionReaction } from '../hooks/useSessionReaction';

interface SessionReactionButtonProps {
  sessionId: number;
  reactCount?: number;
  myReaction?: ReactionType | null;
  showLabel?: boolean;
}

export function SessionReactionButton({ sessionId, reactCount = 0, myReaction = null, showLabel }: SessionReactionButtonProps) {
  const { isUpdating, toggleReaction } = useSessionReaction(sessionId);
  const reacted = Boolean(myReaction);

  return (
    <Button
      type="button"
      variant={showLabel ? 'outline' : 'ghost'}
      className={cn('min-h-11 gap-1.5 disabled:opacity-100', !showLabel && 'min-w-11 px-2', reacted && 'text-destructive hover:text-destructive')}
      aria-label="Thích bộ câu hỏi"
      aria-pressed={reacted}
      aria-busy={isUpdating}
      disabled={isUpdating}
      onClick={() => toggleReaction({ reactCount, myReaction })}
    >
      <Heart aria-hidden="true" className={cn(reacted && 'fill-current')} />
      {showLabel && <span>{reacted ? 'Đã thích bộ câu hỏi' : 'Thích bộ câu hỏi'}</span>}
      <span aria-hidden="true" className="tabular-nums">{reactCount}</span>
      <span className="sr-only" aria-live="polite">{reactCount} lượt thích bộ câu hỏi</span>
    </Button>
  );
}
