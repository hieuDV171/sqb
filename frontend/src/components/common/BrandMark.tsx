import { BookOpenCheck } from 'lucide-react';
import { cn } from '@/lib/utils';

interface BrandMarkProps {
  compact?: boolean;
  className?: string;
}

export function BrandMark({ compact = false, className }: BrandMarkProps) {
  return (
    <span className={cn('inline-flex min-w-0 items-center gap-3', className)}>
      <span className="relative grid size-10 shrink-0 place-items-center rounded-xl bg-primary text-primary-foreground shadow-[var(--shadow-primitive-sm)]">
        <BookOpenCheck className="size-5" aria-hidden="true" />
        <span className="absolute -right-0.5 -top-0.5 size-3 rounded-full border-2 border-card bg-academic" />
      </span>
      {!compact && (
        <span className="min-w-0 leading-none">
          <span className="block truncate text-[1.05rem] font-bold tracking-[-0.025em] text-foreground">
            SQB
          </span>
          <span className="mt-1 block truncate text-[0.68rem] font-semibold uppercase tracking-[0.12em] text-muted-foreground">
            Campus Intelligence
          </span>
        </span>
      )}
    </span>
  );
}
