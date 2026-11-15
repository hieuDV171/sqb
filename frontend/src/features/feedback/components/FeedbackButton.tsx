import { MessageSquarePlus } from 'lucide-react';
import { cn } from '@/lib/utils';

const FEEDBACK_EMAIL = 'bangiaohuongmuadong@gmail.com';

export function FeedbackButton({ floating = false }: { floating?: boolean }) {
  return (
    <a
      href={`mailto:${FEEDBACK_EMAIL}`}
      aria-label="Góp ý qua email"
      title="Mở ứng dụng email để góp ý"
      className={cn(
        'inline-flex h-11 min-w-11 shrink-0 items-center justify-center gap-2 rounded-xl text-sm font-semibold text-muted-foreground transition-colors hover:text-foreground focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-ring',
        floating
          ? 'fixed right-[max(1rem,env(safe-area-inset-right))] bottom-[max(1rem,env(safe-area-inset-bottom))] z-30 border border-border bg-card px-4 shadow-md hover:bg-muted'
          : 'px-2 hover:bg-muted sm:px-3',
      )}
    >
      <MessageSquarePlus className="size-5" aria-hidden="true" />
      <span className={floating ? undefined : 'hidden sm:inline'}>Góp ý</span>
    </a>
  );
}
